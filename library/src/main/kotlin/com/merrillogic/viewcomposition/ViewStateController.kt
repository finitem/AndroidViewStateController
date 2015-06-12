package com.merrillogic.viewcomposition

import android.animation.*
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import rx.Observable
import rx.Subscriber
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList

val defaultInterpolator = AccelerateDecelerateInterpolator()
val defaultDuration = 3500L

data class TransitionComponent(
        val target: View,
        val property: String,
        //interpolator defaults to null, as it will inherit it from the transition if it doesn't have one.
        val interpolator: Interpolator? = null,
        val startVis: Int = View.VISIBLE,
        val endVis: Int = View.VISIBLE,
        val start: Float = 0f,
        val end: Float = 1f,
        val evaluator: TypeEvaluator<Any>? = null,
        val isColor: Boolean = false,
        //TODO: assert that varargs is not empty?
        vararg val values: Any
)


data class Transition(
        val components: ArrayList<TransitionComponent>,
        val duration: Long = defaultDuration,
        val interpolator: Interpolator = defaultInterpolator,
        val actions: TransitionActions = object : TransitionActions {
            override fun before() {
            }

            override fun onStep(progress: Float) {
            }

            override fun after() {
            }
        }
)


//Should the name be stored in the ViewState class at all? Or is that irrelevant?
data class ViewState(
        public val view: View,
        public val visibility: Int,
        public val goals: Set<ViewPropertyGoal>
)

//TODO: Allow for evaluators and/or non-float things? Oh wait, this is JUST the end state - no need for an evaluator. - that's not true, this isn't just the end state, we've also got a "generateDefault" thing goin' on here. we may want the evaluator for that.
//Right now we're assuming a basic animation for the defaults - more advanced things can be manually constructed for real transitions.
// TODO? Add ability to make chained animations and whatnot by describing amount of time that the animation takes, for better default animations? No, wait, yeah, that's bad. THIS IS A STATE, OCCUPYING A SINGLE POINT IN TIME. The evaluator is ONLY for the default animation. And even then, perhaps the creation of a default can be shunted off somewhere else that can handle that.
data class ViewPropertyGoal(
        val property: String,
        val goalValue: Any,
        val evaluator: TypeEvaluator<Any>? = null
)


interface TransitionActions {
    fun before()
    fun onStep(progress: Float)
    fun after()
}


fun makeTransition(viewState: ViewState): Transition {
    val components = ArrayList<TransitionComponent>()
    for (goal in viewState.goals) {
        components.add(TransitionComponent(viewState.view, goal.property, evaluator = goal.evaluator, values = goal.goalValue))
    }
    return Transition(components)
}

fun reverseTransition(transition: Transition): Transition {
    val reverseComponents = ArrayList<TransitionComponent>(transition.components.size())
    for (component in transition.components) {
        reverseComponents.add(
                TransitionComponent(
                        component.target,
                        component.property,
                        component.interpolator,
                        component.endVis,
                        component.startVis,
                        1f - component.end,
                        1f - component.start,
                        component.evaluator,
                        component.isColor,
                        component.values.reverse()))
    }
    return Transition(
            reverseComponents,
            transition.duration,
            transition.interpolator,
            object : TransitionActions {
                override fun before() {
                    transition.actions.after()
                }

                override fun after() {
                    transition.actions.before()
                }

                override fun onStep(progress: Float) {
                    transition.actions.onStep(1f - progress)
                }
            }
    );
}

public class ViewStateController(val name: String, val defaultStateName: String, val states: HashMap<String, ViewState>) {

    class MissingDefaultStateException(val controllerName: String) : RuntimeException("{$controllerName} was created with a default state that is not in its set of states")

    ///TODO: Make each state be able to modify multiple views [important, also simple]
    val transitions = ArrayList<ArrayList<Transition?>>()
    val indices = ArrayList<String>()
    val transitionQueue = LinkedList<Pair<String, Animator>>()

    var currentState: String? = null
    var transitioning = false

    init {
        //populate default transitions
        indices.addAll(states.keySet())
        Collections.sort(indices)
        val size = states.size()
        for (i in 0..(size - 1)) {
            transitions.add(ArrayList(arrayOfNulls<Transition?>(size).asList()))
        }

        if (defaultStateName !in states.keySet()) {
            throw MissingDefaultStateException(name)
        }
    }

    public fun addTransition(transition: Transition, fromState: String, toState: String, reversible: Boolean) {
        val fromIndex = getIndexOfState(fromState)
        val toIndex = getIndexOfState(toState)
        transitions.get(fromIndex).set(toIndex, transition)
        if (reversible) {
            transitions.get(toIndex).set(fromIndex, reverseTransition(transition))
        }
    }

    fun getIndexOfState(state: String) = Collections.binarySearch(indices, state)

    fun getTransition(fromState: String, toState: String): Transition {
        val fromIndex = getIndexOfState(fromState)
        val toIndex = getIndexOfState(toState)
        var transition: Transition? = transitions[fromIndex][toIndex]
        if (transition == null) {
            transition = makeTransition(states[indices[toIndex]])
            transitions[fromIndex][toIndex] = transition
        }
        return transition
    }

    public fun show(stateName: String? = null) {
        enqueue(stateName ?: defaultStateName)
    }

    //TODO: Transitions between A and B that are left to default shouldn't just use B's to default - they need to be a combo of the two ViewStates.

    fun enqueue(toState: String) {
        //TODO: Get this in a bg thread that unthreads when it calls next.
        synchronized(transitionQueue) {
            transitionQueue.add(
                    Pair(toState,
                            makeAnimator(
                                    getTransition(
                                            if (transitionQueue.isEmpty())
                                                currentState ?: defaultStateName
                                            else transitionQueue.peekLast().first,
                                            toState))))
        }
        next()
    }

    synchronized fun next() {
        if (!transitioning && transitionQueue.isNotEmpty()) {
            val pair = transitionQueue.pollFirst()

            transitionStarted(pair.first)
            pair.second.start()
        }
    }

    fun transitionComplete() {
        transitioning = false
        next()
    }

    fun transitionStarted(state: String) {
        currentState = state
        transitioning = true
    }

    fun makeAnimator(transition: Transition): AnimatorSet {
        val set = AnimatorSet()
        set.setDuration(transition.duration)
        val progressAnimator = ValueAnimator.ofFloat(0f, 1f)
        progressAnimator.setInterpolator(transition.interpolator)
        progressAnimator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animator: ValueAnimator) {
                transition.actions.onStep(animator.getAnimatedValue() as Float)
            }
        })

        Observable.from(transition.components)
                .map({
                    component ->
                    {
                        if (component.values.isNotEmpty()) {
                            var each: ObjectAnimator? = null
                            if (component.evaluator != null) {
                                each = ObjectAnimator.ofObject(component.target, component.property, component.evaluator, *(component.values))
                            } else {
                                if (component.values[0] is Int) {
                                    if (component.isColor) {
                                        each = ObjectAnimator.ofArgb(component.target, component.property, *((component.values.filter({ it is Int }) as ArrayList<Int>).toIntArray()))
                                    } else {
                                        each = ObjectAnimator.ofInt(component.target, component.property, *((component.values.filter({ it is Int }) as ArrayList<Int>).toIntArray()))
                                    }
                                } else if (component.values[0] is Float) {
                                    each = ObjectAnimator.ofFloat(component.target, component.property, *((component.values.filter({ it is Float }) as ArrayList<Float>).toFloatArray()))
                                }
                            }
                            if (each != null) {
                                each.setStartDelay((component.start * transition.duration).toLong())
                                each.setDuration(((component.end - component.start) * transition.duration).toLong())
                                each.setInterpolator(component.interpolator)
                                each.addListener(object : AnimatorListenerAdapter() {
                                    //TODO: For convenience, could perhaps add a cleanupvis structure which maintains views
                                    // to start and end vis for entire animation. Right now if all someone wants is it to
                                    // disappear at end of animation but nothing else, then they either call it in the
                                    // after() [not bad, honestly], or make a dummy animation [bad, honestly]
                                    override fun onAnimationStart(animator: Animator) {
                                        component.target.setVisibility(component.startVis)
                                    }

                                    override fun onAnimationEnd(animator: Animator) {
                                        component.target.setVisibility(component.endVis)
                                    }
                                })
                                //Have to cast to ValueAnimator for now to get the types all lined up for the observables
                                //TODO: Figure out better way to indicate this
                                // (I'm a bit surprised that Kotlin can't find the parent in common and use it?
                                // Probably a good reason not to and to force me to be explicit)
                                each
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }.invoke()
                })
                .filter({ item -> item != null })
                .map({ item -> item as ValueAnimator })
                .startWith(progressAnimator)
                //Any actions that need to be applied to all animations occur here
                .toList()
                .subscribe(object : Subscriber<List<ValueAnimator>>() {
                    override fun onError(e: Throwable?) {
                        Log.e("error", "error", e)
                    }

                    override fun onNext(t: List<ValueAnimator>?) {
                        set.playTogether(t)
                    }

                    override fun onCompleted() {

                    }
                }
                )
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                // We specifically do not want to call transitionStarted from here.
                // We want to let the controlling class handle that.
                // We only call transitionComplete because we are the only ones who know when that finishes.
                transition.actions.before()
            }

            override fun onAnimationEnd(animation: Animator) {
                transition.actions.after()
                transitionComplete()
            }
        })
        return set
    }
}