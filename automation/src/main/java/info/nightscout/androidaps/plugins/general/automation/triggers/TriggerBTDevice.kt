package info.nightscout.androidaps.plugins.general.automation.triggers

import android.bluetooth.BluetoothManager
import android.content.Context
import android.widget.LinearLayout
import com.google.common.base.Optional
import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.automation.R
import info.nightscout.androidaps.events.EventBTChange
import info.nightscout.androidaps.plugins.general.automation.AutomationPlugin
import info.nightscout.androidaps.plugins.general.automation.elements.ComparatorConnect
import info.nightscout.androidaps.plugins.general.automation.elements.InputDropdownMenu
import info.nightscout.androidaps.plugins.general.automation.elements.LayoutBuilder
import info.nightscout.androidaps.plugins.general.automation.elements.StaticLabel
import info.nightscout.androidaps.utils.JsonHelper
import info.nightscout.shared.logging.LTag
import org.json.JSONObject
import java.util.*
import javax.inject.Inject

class TriggerBTDevice(injector: HasAndroidInjector) : Trigger(injector) {

    @Inject lateinit var context: Context
    @Inject lateinit var automationPlugin: AutomationPlugin

    var btDevice = InputDropdownMenu(rh, "")
    var comparator: ComparatorConnect = ComparatorConnect(rh)

    private constructor(injector: HasAndroidInjector, triggerBTDevice: TriggerBTDevice) : this(injector) {
        comparator = ComparatorConnect(rh, triggerBTDevice.comparator.value)
        btDevice.value = triggerBTDevice.btDevice.value
    }

    @Synchronized
    override fun shouldRun(): Boolean {
        if (eventExists()) {
            aapsLogger.debug(LTag.AUTOMATION, "Ready for execution: " + friendlyDescription())
            return true
        }
        return false
    }

    override fun dataJSON(): JSONObject =
        JSONObject()
            .put("comparator", comparator.value.toString())
            .put("name", btDevice.value)

    override fun fromJSON(data: String): Trigger {
        val d = JSONObject(data)
        btDevice.value = JsonHelper.safeGetString(d, "name")!!
        comparator.value = ComparatorConnect.Compare.valueOf(JsonHelper.safeGetString(d, "comparator")!!)
        return this
    }

    override fun friendlyName(): Int = R.string.btdevice

    override fun friendlyDescription(): String =
        rh.gs(R.string.btdevicecompared, btDevice.value, rh.gs(comparator.value.stringRes))

    override fun icon(): Optional<Int> = Optional.of(R.drawable.ic_bluetooth_white_48dp)

    override fun duplicate(): Trigger = TriggerBTDevice(injector, this)

    override fun generateDialog(root: LinearLayout) {
        val pairedDevices = devicesPaired()
        btDevice.setList(pairedDevices)
        LayoutBuilder()
            .add(StaticLabel(rh, R.string.btdevice, this))
            .add(btDevice)
            .add(comparator)
            .build(root)
    }

    // Get the list of paired BT devices to use in dropdown menu
    private fun devicesPaired(): ArrayList<CharSequence> {
        val s = ArrayList<CharSequence>()
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter?.bondedDevices?.forEach { s.add(it.name) }
        return s
    }

    private fun eventExists(): Boolean {
        automationPlugin.btConnects.forEach {
            if (btDevice.value == it.deviceName) {
                if (comparator.value == ComparatorConnect.Compare.ON_CONNECT && it.state == EventBTChange.Change.CONNECT) return true
                if (comparator.value == ComparatorConnect.Compare.ON_DISCONNECT && it.state == EventBTChange.Change.DISCONNECT) return true
            }
        }
        return false
    }
}