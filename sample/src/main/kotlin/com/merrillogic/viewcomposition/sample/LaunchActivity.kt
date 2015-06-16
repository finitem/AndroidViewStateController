package com.merrillogic.viewcomposition.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.merrillogic.viewcomposition.ViewPropertyGoal
import com.merrillogic.viewcomposition.ViewState
import com.merrillogic.viewcomposition.ViewStateController
import java.util.HashMap


public class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        val hello = findViewById(R.id.hello)
        val goodbye = findViewById(R.id.goodbye)
        val controller = ViewStateController(
                "TestController",
                "zero",
                HashMap(mapOf(
                        "zero" to listOf(
                                ViewState(
                                        hello,
                                        View.VISIBLE,
                                        setOf(
                                                ViewPropertyGoal("translationX", 40f),
                                                ViewPropertyGoal("translationY", 100f)
                                        )
                                ),
                                ViewState(
                                        goodbye,
                                        View.VISIBLE,
                                        setOf(
                                                ViewPropertyGoal("rotation", 30f)
                                        )
                                )
                        ),
                        "one" to listOf(
                                ViewState(
                                        hello,
                                        View.VISIBLE,
                                        setOf(
                                                ViewPropertyGoal("alpha", .2f),
                                                ViewPropertyGoal("translationY", 50f)
                                        )
                                ),
                                ViewState(
                                        goodbye,
                                        View.VISIBLE,
                                        setOf(
                                                ViewPropertyGoal("rotation", 0f)
                                        )
                                )
                        ),
                        "two" to listOf(
                                ViewState(
                                        hello,
                                        View.GONE,
                                        setOf(
                                                ViewPropertyGoal("alpha", 1f),
                                                ViewPropertyGoal("translationX", 0f)
                                        )
                                )
                        )
                ))
        )
        //For testing purposes, should really implement some form of infitinitely repeating animation,
        // then interrupt it midstream and whatnot.
        controller.show("zero")
        controller.show("one")
        controller.show("two")
        controller.show("two")
        controller.show("zero")
        //TODO: Tests.
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_launch, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item!!.getItemId()

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
