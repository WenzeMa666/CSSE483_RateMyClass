package edu.rosehulman.ratemyclass

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import edu.rosehulman.rosefire.Rosefire
import edu.rosehulman.rosefire.RosefireResult
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity(),
                     DepartmentListFragment.OnDepartmentSelectedListener,
                     CourseListFragment.OnCourseSelectedListener,
                     SplashFragment.OnLoginButtonPressedListener,
                     SearchFragment.OnSearchListener,
                     profileFragment.OnButtonClicked,
                     CourseDetailFragment.OnOKButtonPressed{

    private val auth = FirebaseAuth.getInstance()
    lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private val RC_ROSEFIRE_LOGIN = 1001
    private val REGISTRY_TOKEN = "53a4fea9-cc87-44f4-9aae-209bf3891579"

    private var course: Course? = null
    private var department: Department? = null

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
                Log.d("AAA", user.uid)
                User.username = user.uid
                InitialImages.loadRandomInitialImages()
            }
            goToSearchPage()
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

    fun getFab(): FloatingActionButton {
        return fab
    }

    private fun switchToSplashFragment() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, SplashFragment())
        ft.commit()
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

    private fun goToCoursePage(dept: Department?, course: Course?) {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, CourseListFragment(dept, course))
        while (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
        ft.addToBackStack("detail")
        ft.commit()
    }

    private fun goToProfilePage() {
        if (User.username != "") {
            val ft = supportFragmentManager.beginTransaction()
            ft.replace(R.id.fragment_container, profileFragment())
            ft.commit()
        } else {
            switchToSplashFragment()
        }
    }

    override fun onDepartmentSelected(dept: Department) {
        Log.d("AAA", "Document selected: ${dept.deptName}")
        goToCoursePage(dept, null)
    }

    override fun onCourseSelected(dept: Department, course: Course) {
        Log.d("AAA", "Document selected: ${course.courseName}")
        val courseDetailFragment = CourseDetailFragment.newInstance(course, dept)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, courseDetailFragment)
        ft.addToBackStack("detail")
        ft.commit()
    }

    override fun onLoginButtonPressed() {
        val signInIntent: Intent = Rosefire.getSignInIntent(this, REGISTRY_TOKEN)
        startActivityForResult(signInIntent, RC_ROSEFIRE_LOGIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_ROSEFIRE_LOGIN) {
            val result: RosefireResult = Rosefire.getSignInResultFromIntent(data)
            if (result.isSuccessful) {
                Log.d("AAA", result.username)
                auth.signInWithCustomToken(result.token).addOnCompleteListener(this, OnCompleteListener {
                    Log.d("AAA", "Login succeeded")
                })
            }
        }
    }

    override fun onClassSearched(classSearched: String) {
        val coursesRef: CollectionReference = FirebaseFirestore
            .getInstance()
            .collection("Course")

        coursesRef.whereEqualTo("courseName", classSearched)
            .get()
            .addOnSuccessListener {documents ->
                for (doc in documents) {
                    course = Course.fromSnapshot(doc)
                }
                val deptRef: CollectionReference = FirebaseFirestore
                    .getInstance()
                    .collection("Department")

                deptRef.whereEqualTo("abbr", course?.dept)
                    .get()
                    .addOnSuccessListener { documents->
                        for (doc in documents) {
                            department = Department.fromSnapshot(doc)
                        }
                        if (classSearched != "" && (course == null || department == null)) {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Course Not Found")

                            val view = LayoutInflater.from(this).inflate(R.layout.not_found_view, null, false)
                            builder.setView(view)

                            builder.setPositiveButton(android.R.string.ok) {_,_ ->

                            }

                            builder.setNegativeButton(android.R.string.cancel, null)
                            builder.create().show()
                        } else if (course != null || department != null){
                            goToCoursePage(department, course)
                        }
                    }

            }
    }

    override fun onButtonClicked() {
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, MyComments())
        while (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
        ft.addToBackStack("detail")
        ft.commit()
    }

    override fun onOKButtonPressed() {
        goToProfilePage()
    }

    override fun sendNotification() {
        var builder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Title")
            .setContentText("Content")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        createNotificationChannel()

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(0, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Channel Name"
            val descriptionText = "Channel description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
