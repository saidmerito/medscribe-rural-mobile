package dj.medscriberural.companion

import android.app.Application
import dj.medscriberural.companion.data.AppDatabase

class MedScribeApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
