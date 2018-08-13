package jp.netkids.fingerprint

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "TEST"
    }

    private var mDialog: Dialog? = null
    private var mCancellationSignal: CancellationSignal? = null
    private var mKeyguardManager: KeyguardManager? = null
    private var mFingerprintManager: FingerprintManager? = null
    val REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mKeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mFingerprintManager = getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        }
        button.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isSupportFingerPrint()) {
                    useFingerprint()
                    return@setOnClickListener
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isSupportScreenLock()) {
                    showAuthenticationScreen()
                    return@setOnClickListener
                }
            }
            Log.d(TAG, "デバイス認証利用できない状態")
        }
        button2.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isSupportScreenLock()) {
                    showAuthenticationScreen()
                    return@setOnClickListener
                }
            }
            Log.d(TAG, "デバイス認証利用できない状態")
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun isSupportFingerPrint(): Boolean {
        if (checkSelfPermission(Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (mFingerprintManager!!.isHardwareDetected || mFingerprintManager!!.hasEnrolledFingerprints()) {
            return true
        }
        return false
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun useFingerprint() {
        if (isSupportFingerPrint()) {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("本人確認")
            builder.setMessage("本人確認のため、指紋センサーに触れてください")
            builder.setNegativeButton("キャンセル", DialogInterface.OnClickListener { dialog, which -> mCancellationSignal!!.cancel() })
            builder.setOnCancelListener(DialogInterface.OnCancelListener { mCancellationSignal!!.cancel() })
            mDialog = builder.show()

            mCancellationSignal = CancellationSignal()

            mFingerprintManager!!.authenticate(null, mCancellationSignal!!, 0, object : FingerprintManager.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(this@MainActivity, "復帰不可能なエラーを検出したとき: $errString", Toast.LENGTH_SHORT).show()
                    mDialog!!.dismiss()
                }

                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence) {
                    Toast.makeText(this@MainActivity, "復帰可能なエラーを検出したとき: $helpString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult) {
                    Toast.makeText(this@MainActivity, "認証成功", Toast.LENGTH_SHORT).show()
                    mDialog!!.dismiss()
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(this@MainActivity, "認証失敗", Toast.LENGTH_SHORT).show()
                }
            }, Handler())
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun isSupportScreenLock(): Boolean {
        return (mKeyguardManager!!.isKeyguardSecure())
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun showAuthenticationScreen() {
        // デバイス認証画面のタイトルと説明文は変更することが可能
        val intent = mKeyguardManager!!.createConfirmDeviceCredentialIntent("本人確認", "本人確認にため、認証が必要です。")
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this@MainActivity, "認証成功", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("TEST", "キャンセル")
                return
            }
        }
    }
}
