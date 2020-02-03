package edu.rosehulman.ratemyclass

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(),
                     DepartmentListFragment.OnDepartmentSelectedListener,
                     CourseListFragment.OnCourseSelectedListener,
                     SplashFragment.OnLoginButtonPressedListener {

    private val auth = FirebaseAuth.getInstance()
    lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private val RC_SIGN_IN = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        goToSearchPage()

        nav_view.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    goToSearchPage()
                    true

                }
                R.id.navigation_dashboard -> {
                    goToDeptPage()
                    true
                }
                R.id.navigation_notifications -> {
                    goToProfilePage()
                    true
                }
                else -> false
            }
        }

        initializeListeners()

        fab.hide()
    }

    private fun initializeListeners() {
        authStateListener = FirebaseAuth.AuthStateListener { auth: FirebaseAuth ->
            val user = auth.currentUser
            Log.d("AAA", "In auth listener, user = $user")
            if (user != null) {
                goToSearchPage()
            } else {
                switchToSplashFragment()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }
    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }


    private fun switchToSplashFragment() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, SplashFragment())
        ft.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun goToSearchPage() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, SearchFragment())
        while (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
        ft.commit()
    }

    private fun goToDeptPage() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, DepartmentListFragment())
        while (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
        ft.commit()
    }

    private fun goToCoursePage(dept: Department) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, CourseListFragment(dept))
        while (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
        ft.addToBackStack("detail")
        ft.commit()
    }

    private fun goToProfilePage() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, profileFragment())
        ft.commit()
    }

    override fun onDepartmentSelected(dept: Department) {
        Log.d("AAA", "Document selected: ${dept.deptName}")
        goToCoursePage(dept)
    }

    override fun onCourseSelected(dept: Department, course: Course) {
        Log.d("AAA", "Document selected: ${course.courseName}")
    }

    override fun onLoginButtonPressed() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        val loginIntent =  AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.ic_launcher_custom)
            .build()

        // Create and launch sign-in intent
        startActivityForResult(loginIntent, RC_SIGN_IN)
    }
}
