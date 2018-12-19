package com.machichiotte.pocqrcodedecathlon

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.LoaderManager.LoaderCallbacks
import android.content.Context
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.zxing.BarcodeFormat
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.BASE_URL
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.PREFS_ID
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.REQUEST_CAMERA_ACCESS
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.REQUEST_READ_CONTACTS
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.SUCCESS
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.USER_BASE_URL
import com.machichiotte.pocqrcodedecathlon.Constants.Constants.USER_TOKEN
import com.machichiotte.pocqrcodedecathlon.pojo.ApiJerem
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity(), LoaderCallbacks<Cursor> {
    private var mAuthTask: UserLoginTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up the login form.
        populateAutoComplete()
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener { attemptLogin() }

        tv_uuid.text = "uuid : ${UUID.randomUUID()}"

        prepareToolbar()

        checkReadPerm()
    }

    private fun prepareToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_action_bar_login, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        when (item.itemId) {
            R.id.action_settings -> {
                openSettings()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun openSettings() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val nullParent: ViewGroup? = null
        val dialogView = inflater.inflate(R.layout.custom_dialog, nullParent)
        dialogBuilder.setView(dialogView)

        val sharedPref = this.getSharedPreferences(PREFS_ID, MODE_PRIVATE)

        val edt = dialogView.findViewById(R.id.edit1) as EditText
        edt.setText(checkBaseUrl())

        dialogBuilder.setTitle(getString(R.string.login_dialog_title))
        dialogBuilder.setPositiveButton(getString(R.string.accept)) { _, _ ->
            with(sharedPref.edit()) {
                if (edt.text.substring(edt.text.length - 1) != "/") {
                    putString(USER_BASE_URL, edt.text.toString() + "/")
                } else
                    putString(USER_BASE_URL, edt.text.toString())
                apply()
            }
            //}

        }
        dialogBuilder.setNeutralButton(getString(R.string.reset)) { _, _ ->
            with(sharedPref.edit()) {
                putString(USER_BASE_URL, BASE_URL)
                apply()
            }
        }

        dialogBuilder.setNegativeButton(getString(R.string.cancel)) { _, _ ->
            //pass
        }

        val b = dialogBuilder.create()

        edt.addTextChangedListener(object : TextWatcher {
            fun handleText() {
                // Grab the button
                val okButton: Button = b.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.isEnabled =
                        !(edt.text.isEmpty() || (!edt.text.toString().contains("http://") && !edt.text.toString().contains(
                            "https://"
                        )))

                if (okButton.isEnabled) {
                    b.getButton(AlertDialog.BUTTON_POSITIVE).alpha = 1f
                    b.getButton(AlertDialog.BUTTON_POSITIVE).isClickable = true
                } else {
                    b.getButton(AlertDialog.BUTTON_POSITIVE).alpha = .5f
                    b.getButton(AlertDialog.BUTTON_POSITIVE).isClickable = false
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                handleText()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        b.show()

        b.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        b.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        b.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
    }

    private fun checkReadPerm() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_CONTACTS),
                    REQUEST_READ_CONTACTS
                )
            }
        }
    }

    private fun populateAutoComplete() {
        loaderManager.initLoader(0, null, this)
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }

        if (requestCode == REQUEST_CAMERA_ACCESS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            }
        }
    }

    private fun closeKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(password.windowToken, 0)
        imm.hideSoftInputFromWindow(email.windowToken, 0)
    }

    private fun launchCamera() {
        val i = Intent(this@LoginActivity, SimpleScannerActivity::class.java)
        i.putExtra("SCAN_FORMATS", BarcodeFormat.QR_CODE.toString())
        i.putExtra("SCAN_FORMATS", BarcodeFormat.CODABAR.toString())
        startActivity(i)
        finish()
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {

        closeKeyboard()

        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        email.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(emailStr, passwordStr)
            mAuthTask!!.execute(null as Void?)

        }
    }

    private fun checkLoginPassword(emailStr: String, passwordStr: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(checkBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(InterfaceApi::class.java)

        val call: Call<ApiJerem> = service.getLoginAuth(emailStr, passwordStr)

        call.enqueue(object : Callback<ApiJerem> {
            override fun onResponse(call: Call<ApiJerem>, response: Response<ApiJerem>) {

                response.body()?.let {
                    if (it.status == SUCCESS) {
                        saveToken(it.token)
                        checkCameraPermission()
                    } else {
                        Toast.makeText(this@LoginActivity, it.error_message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiJerem>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Something went wrong...Please try later!", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun saveToken(token: String?) {
        token?.let {
            val sharedPref = this.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putString(USER_TOKEN, it)
                apply()
            }
        }
    }


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.CAMERA),
                    REQUEST_CAMERA_ACCESS
                )
            }
        } else {
            launchCamera()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        login_form.visibility = if (show) View.GONE else View.VISIBLE
        login_form.animate()
            .setDuration(shortAnimTime)
            .alpha((if (show) 0 else 1).toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    login_form.visibility = if (show) View.GONE else View.VISIBLE
                }
            })

        login_progress.visibility = if (show) View.VISIBLE else View.GONE
        login_progress.animate()
            .setDuration(shortAnimTime)
            .alpha((if (show) 1 else 0).toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    login_progress.visibility = if (show) View.VISIBLE else View.GONE
                }
            })

    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(
            this,
            // Retrieve data rows for the device user's 'profile' contact.
            Uri.withAppendedPath(
                ContactsContract.Profile.CONTENT_URI,
                ContactsContract.Contacts.Data.CONTENT_DIRECTORY
            ), ProfileQuery.PROJECTION,

            // Select only email addresses.
            ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(
                ContactsContract.CommonDataKinds.Email
                    .CONTENT_ITEM_TYPE
            ),

            // Show primary email addresses first. Note that there won't be
            // a primary email address if the user hasn't specified one.
            ContactsContract.Contacts.Data.IS_PRIMARY + " DESC"
        )
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }

    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(
            this@LoginActivity,
            android.R.layout.simple_dropdown_item_1line, emailAddressCollection
        )

        email.setAdapter(adapter)
    }

    object ProfileQuery {
        val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.IS_PRIMARY
        )
        val ADDRESS = 0
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask internal constructor(private val mEmail: String, private val mPassword: String) :
        AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean? {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return false
            }

            return true
        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
                checkLoginPassword(mEmail, mPassword)
            } else {
                password.error = getString(R.string.error_incorrect_password)
                password.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }

    private fun checkBaseUrl(): String {
        var baseUrl = BASE_URL

        getSharedPreferences(PREFS_ID, MODE_PRIVATE).getString(USER_BASE_URL, null)?.let {
            baseUrl = it
        }

        return baseUrl
    }

}
