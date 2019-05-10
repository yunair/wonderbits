package com.air.bledemo

import android.bluetooth.BluetoothDevice
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item.view.*

class BleDeviceAdapter(devices: ArrayList<BluetoothDevice>?) : RecyclerView.Adapter<BleDeviceAdapter.ViewHolder>() {
    private val devices: ArrayList<BluetoothDevice> = devices ?: arrayListOf()
    private var action: ((BluetoothDevice) -> Unit)? = null
    fun setClickAction(action: ((BluetoothDevice) -> Unit)?) {
        this.action = action
    }

    fun add(device: BluetoothDevice?) {
        device?.let {
            devices.add(it)
            notifyDataSetChanged()
        }
    }

    fun addAll(devices: List<BluetoothDevice>?) {
        devices?.let {
            this.devices.addAll(it)
            notifyDataSetChanged()
        }
    }

    fun set(devices: List<BluetoothDevice>?) {
        this.devices.clear()
        devices?.let {
            this.devices.addAll(it)
            notifyDataSetChanged()
        }
    }

    fun clear() {
        devices.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View.inflate(parent.context, R.layout.item, null)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setContent(devices[position])
    }

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun setContent(vo: BluetoothDevice) {
            with(itemView) {
                name.text = vo.name ?: ""
                content.text = vo.address ?: ""
                setOnClickListener {
                    action?.invoke(vo)
                }
            }
        }
    }
}