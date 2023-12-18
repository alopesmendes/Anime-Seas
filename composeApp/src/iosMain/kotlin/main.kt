import androidx.compose.ui.window.ComposeUIViewController
import com.ailtontech.animeseas.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
