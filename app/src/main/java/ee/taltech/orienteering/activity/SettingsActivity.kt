package ee.taltech.orienteering.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import ee.taltech.orienteering.R
import ee.taltech.orienteering.api.controller.AccountController
import ee.taltech.orienteering.api.controller.TrackSyncController
import ee.taltech.orienteering.api.dto.LoginDto
import ee.taltech.orienteering.api.dto.RegisterDto
import ee.taltech.orienteering.component.spinner.adapter.TrackTypeSpinnerAdapter
import ee.taltech.orienteering.db.domain.User
import ee.taltech.orienteering.db.repository.*
import ee.taltech.orienteering.track.TrackType
import ee.taltech.orienteering.util.HashUtils
import ee.taltech.orienteering.util.TrackUtils

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val BUNDLE_IS_REGISTER = "toggle_register"

        private const val MAX_SYNC_INTERVAL = 60 * 1000L
    }

    private val accountController = AccountController.getInstance(this)
    private val trackSyncController = TrackSyncController.getInstance(this)

    private val userRepository = UserRepository.open(this)
    private val offlineSessionsRepository = OfflineSessionsRepository.open(this)
    private val trackSummaryRepository = TrackSummaryRepository.open(this)
    private val trackLocationsRepository = TrackLocationsRepository.open(this)
    private val checkpointsRepository = CheckpointsRepository.open(this)
    private val wayPointsRepository = WayPointsRepository.open(this)

    private var user: User? = null
    private var isRegister = false

    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextFirstName: EditText
    private lateinit var editTextLastName: EditText

    private lateinit var textTitle: TextView
    private lateinit var textUsernameLbl: TextView
    private lateinit var editTextEmailLbl: TextView
    private lateinit var editTextPasswordLbl: TextView
    private lateinit var textFirstNameLbl: TextView
    private lateinit var textLastNameLbl: TextView

    private lateinit var layoutRegister: ConstraintLayout
    private lateinit var layoutSettings: ConstraintLayout

    private lateinit var switchAutoSync: Switch
    private lateinit var switchSpeedMode: Switch
    private lateinit var seekBarSyncInterval: SeekBar
    private lateinit var seekBarSyncLabel: TextView

    private lateinit var buttonRegister: Button
    private lateinit var buttonSyncTrack: Button
    private lateinit var buttonToggleRegister: Button
    private lateinit var buttonLogOut: Button

    private lateinit var spinnerTrackType: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        actionBar?.hide()
        supportActionBar?.hide()

        setContentView(R.layout.activity_settings)

        textTitle = findViewById(R.id.txt_title)
        editTextUsername = findViewById(R.id.txt_username)
        editTextEmail = findViewById(R.id.txt_email)
        editTextPassword = findViewById(R.id.txt_password)
        editTextFirstName = findViewById(R.id.txt_first_name)
        editTextLastName = findViewById(R.id.txt_last_name)
        buttonRegister = findViewById(R.id.btn_register)
        textUsernameLbl = findViewById(R.id.txt_username_lbl)
        textFirstNameLbl = findViewById(R.id.txt_first_name_lbl)
        textLastNameLbl = findViewById(R.id.txt_last_name_lbl)
        buttonToggleRegister = findViewById(R.id.toggle_register)

        layoutRegister = findViewById(R.id.layout_register)
        layoutSettings = findViewById(R.id.layout_settings)

        switchAutoSync = findViewById(R.id.switch_auto_sync)
        switchSpeedMode = findViewById(R.id.switch_speed_pace)
        seekBarSyncInterval = findViewById(R.id.seek_bar_sync_interval)
        seekBarSyncLabel = findViewById(R.id.seek_bar_sync_label)

        buttonSyncTrack = findViewById(R.id.btn_sync_track)
        buttonLogOut = findViewById(R.id.btn_logout)

        spinnerTrackType = findViewById(R.id.spinner_track_type)

        switchSpeedMode.setOnCheckedChangeListener { _, isChecked -> onSpeedModeChange(isChecked) }
        switchAutoSync.setOnCheckedChangeListener { _, isChecked -> onAutoSyncChange(isChecked) }
        seekBarSyncInterval.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                val value = i * (seekBar.width - 2 * seekBar.thumbOffset) / seekBar.max
                seekBarSyncLabel.text = i.toString()
                seekBarSyncLabel.measure(0, 0)
                val textWidth =  seekBarSyncLabel.measuredWidth
                val firstRemainder = seekBar.thumbOffset - textWidth
                val result = firstRemainder / 2
                seekBarSyncLabel.x = (seekBar.x + value + result)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onSyncIntervalChanged()
            }
        })

        buttonRegister.setOnClickListener { onRegister() }
        buttonToggleRegister.setOnClickListener { onToggleRegister() }
        buttonSyncTrack.setOnClickListener { onTrackSync() }
        buttonLogOut.setOnClickListener { onLogout() }

        user = userRepository.readUser()

        if (user == null) {
            layoutRegister.visibility = View.VISIBLE
            layoutSettings.visibility = View.GONE
            switchAutoSync.isChecked = true
            switchSpeedMode.isChecked = true
        } else {
            layoutRegister.visibility = View.GONE
            layoutSettings.visibility = View.VISIBLE

            switchAutoSync.isChecked = user!!.autoSync
            switchSpeedMode.isChecked = !user!!.speedMode
            seekBarSyncInterval.progress = ((MAX_SYNC_INTERVAL - user!!.syncInterval) / 1000).toInt()

            accountController.login(LoginDto(user!!.email, user!!.password + "5x-9#Y6Cpp"))
        }
    }

    override fun onStart() {
        super.onStart()

        if (user != null) {
            accountController.login(LoginDto(user!!.email, user!!.password + "5x-9#Y6Cpp"))
        }
        // onLogin()
        setUpTypeSpinner(spinnerTrackType)

        overridePendingTransition(
            R.anim.slide_in_from_left,
            R.anim.slide_out_to_right
        )
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(
            R.anim.slide_in_from_right,
            R.anim.slide_out_to_left
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        userRepository.close()
        offlineSessionsRepository.close()
        trackSummaryRepository.close()
        trackSummaryRepository.close()
        checkpointsRepository.close()
        wayPointsRepository.close()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(BUNDLE_IS_REGISTER, isRegister)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isRegister = !savedInstanceState.getBoolean(BUNDLE_IS_REGISTER)
        onToggleRegister()
    }

    // ====================================== REGISTER LOGIC ===========================================

    private fun onLogout() {
        userRepository.deleteUsers()
        layoutSettings.visibility = View.GONE
        layoutRegister.visibility = View.VISIBLE
    }

    private fun onToggleRegister() {
        isRegister = !isRegister
        if (isRegister) {
            buttonRegister.text = getString(R.string.log_in)
            buttonToggleRegister.text = getString(R.string.register_instead)
            textTitle.text = getString(R.string.log_in)
            editTextUsername.visibility = View.GONE
            editTextFirstName.visibility = View.GONE
            editTextLastName.visibility = View.GONE
            textUsernameLbl.visibility = View.GONE
            textFirstNameLbl.visibility = View.GONE
            textLastNameLbl.visibility = View.GONE
            buttonRegister.setOnClickListener { onLogin() }
        } else {
            buttonRegister.text = getString(R.string.register)
            buttonToggleRegister.text = getString(R.string.login_instead)
            textTitle.text = getString(R.string.register)
            editTextUsername.visibility = View.VISIBLE
            editTextFirstName.visibility = View.VISIBLE
            editTextLastName.visibility = View.VISIBLE
            textUsernameLbl.visibility = View.VISIBLE
            textFirstNameLbl.visibility = View.VISIBLE
            textLastNameLbl.visibility = View.VISIBLE
            buttonRegister.setOnClickListener { onRegister() }
        }
    }

    private fun onLogin() {
        val loginDto = LoginDto(editTextEmail.text.toString(), HashUtils.md5(editTextPassword.text.toString()) + "5x-9#Y6Cpp")
        accountController.login(loginDto) { response ->
            val newUser = User(
                null,
                "Not Implemented in backend",
                editTextEmail.text.toString(),
                HashUtils.md5(editTextPassword.text.toString()),
                response.firstName,
                response.lastName
            )
            userRepository.saveUser(newUser)
            layoutRegister.visibility = View.GONE
            layoutSettings.visibility = View.VISIBLE
            user = newUser
        }
    }

    private fun onRegister() {
        if (editTextEmail.text.length < 3 || !editTextEmail.text.contains('@')) {
            Snackbar.make(findViewById(R.id.activity_settings), "Invalid email!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (editTextPassword.text.length < 8
            || editTextPassword.text.toString().matches(Regex("^[a-zA-Z0-9]*$"))
            || !editTextPassword.text.toString().matches(Regex(".*[a-z].*"))
            || !editTextPassword.text.toString().matches(Regex(".*[A-Z].*"))
        ) {
            Snackbar.make(findViewById(R.id.activity_settings), "Invalid password!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (editTextLastName.text.isEmpty()) {
            Snackbar.make(findViewById(R.id.activity_settings), "Last name is required!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (editTextFirstName.text.isEmpty()) {
            Snackbar.make(findViewById(R.id.activity_settings), "First name is required!", Snackbar.LENGTH_LONG).show()
            return
        }
        val registerDto = RegisterDto(
            editTextEmail.text.toString(),
            HashUtils.md5(editTextPassword.text.toString()) + "5x-9#Y6Cpp",
            editTextFirstName.text.toString(),
            editTextLastName.text.toString()
        )

        val user = User(
            null,
            editTextUsername.text.toString(),
            registerDto.email,
            HashUtils.md5(editTextPassword.text.toString()),
            registerDto.firstName,
            registerDto.lastName
        )

        accountController.createAccount(registerDto) {
            userRepository.saveUser(user)
            layoutRegister.visibility = View.GONE
            layoutSettings.visibility = View.VISIBLE
        }
    }

    // ========================================== HELPER FUNCTIONS =========================================

    private fun onTrackSync() {
        TrackUtils.syncTracks(
            offlineSessionsRepository,
            trackSummaryRepository,
            trackLocationsRepository,
            checkpointsRepository,
            wayPointsRepository,
            trackSyncController,
            this
        )
    }

    private fun onSpeedModeChange(isOn: Boolean) {
        user?.speedMode = !isOn
        if (user != null) {
            userRepository.updateUser(user!!)
        }
    }

    private fun onAutoSyncChange(isOn: Boolean) {
        user?.autoSync = isOn
        if (user != null) {
            userRepository.updateUser(user!!)
        }
    }

    private fun onSyncIntervalChanged() {
        user?.syncInterval = (MAX_SYNC_INTERVAL - seekBarSyncInterval.progress * 1000).toLong()
        Log.d("", user!!.syncInterval.toString())
        if (user != null) {
            userRepository.updateUser(user!!)
        }
    }

    private fun setUpTypeSpinner(spinner: Spinner) {
        val displayOptionAdapter = TrackTypeSpinnerAdapter(this)
        spinner.adapter = displayOptionAdapter
        spinner.setSelection(user?.defaultActivityType?.value ?: 0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (user != null && user!!.defaultActivityType != TrackType.fromInt(position)) {
                    user!!.defaultActivityType = TrackType.fromInt(position) ?: TrackType.UNKNOWN
                    userRepository.updateUser(user!!)
                    Toast.makeText(this@SettingsActivity, "Changes will apply to next session", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
}
