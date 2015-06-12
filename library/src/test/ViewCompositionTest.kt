package com.merrillogic.viewcomposition

class ViewCompositionTest


fun overallIdea() {
    //Or it can take a linked lifecycle, perhaps.
    val composition: ViewComposition = ViewComposition(rootView);
    //Should the constructor take in a root view? probably.
    //I DO want to instantiate it, because I'm imagining this is called inside of an activity that's getting its stuff set up and whatnot.
    //Uh, I guess within the activity that's all that really wants to do? An activity can define the compositions that make it up, with one likely root composition (but more can be supported?)


    //There are events and requests
}

class SampleComposition(rootView : View) : ViewComposition {
    Inject@
    View myButton

    Inject
    View myText

    fun createStates() {

        mapOf("x" to )
        default : State = State({myButton : ["x" : 10, "y" : 20], myText : ["alpha" : 0f, "y" : 10, VISIBLE : GONE})
        textShown : State = State({myText : ["alpha", 1f]})
    }

    fun createTransitions() {
        component : TransitionComponent = TransitionComponent(view, "x", 15, 20, 30, 35, interpolator = Blah, start = 0, end = .3f)
        defaultToText : Transition = Transition(duration = 350, interpolator = blah, [components])
        //So for transitions, I want to be able to say something like "
    }

}