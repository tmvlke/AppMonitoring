package wskim.aos.appMonitoring

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import wskim.aos.appMonitoring.ui.theme.BlockingAppTheme


class MainActivity : ComponentActivity() {

    private val text = mutableStateOf("")
    private var bound = false

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder: MonitoringForegroundService.LocalBinder = service as MonitoringForegroundService.LocalBinder

            binder.service.data.onEach {
                text.value = getLastPackage(it)
            }.launchIn(CoroutineScope(Dispatchers.Main))

            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }

        private var lastPackage = "none"
        private fun getLastPackage(packageName: String?) : String {

            // 최근 실행된 패키지가 없으면 가장 마지막 패키지 반환
            if (packageName.isNullOrEmpty()) return lastPackage

            // 본 패키지는 수집하지 않음
            if(packageName == "wskim.aos.appMonitoring") return lastPackage

            // 그외에는 들어온 대로 반환하기
            return packageName.apply {
                lastPackage = this
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (checkPermission()) {
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU && PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)){
                // 푸쉬 권한 없음
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + this.packageName)))
            } else {
                Intent(this, MonitoringForegroundService::class.java).also {
                    startService(it)
                    bindService(it, connection, BIND_AUTO_CREATE)
                }
            }
        } else {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    override fun onStop() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BlockingAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(text.value)
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        val appOps = applicationContext
            .getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), applicationContext.packageName
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            applicationContext.checkCallingOrSelfPermission(
                Manifest.permission.PACKAGE_USAGE_STATS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            (mode == AppOpsManager.MODE_ALLOWED)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "마지막 실행 앱: $name",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BlockingAppTheme {
        Greeting("Android")
    }
}