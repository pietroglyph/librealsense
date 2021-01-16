package com.intel.realsense.librealsense;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsbUtilities {
    private static final String TAG = "librs UsbUtilities";
    public static final String ACTION_USB_PERMISSION = "USB_CONTROL_PERMISSION";

    // These PID/VIDs probably only work for non-T200 devices
    private final static int INTEL_VENDOR_ID = 0x8086;
    private final static int INTEL_PRODUCT_ID = 0x0;
    // My T265 only shows with the below VID/PID combo
    private final static int INTEL_RS_VENDOR_ID = 0x8087;
    private final static int INTEL_RS_PRODUCT_ID = 0x0B37;
    // Legacy PID/VIDs... For some reason devices can sometimes get "downgraded"
    private final static int MOVIDIUS_VENDOR_ID = 0x03E7;
    private final static int MOVIDIUS_PRODUCT_ID = 0x2150;

    public static boolean isIntel(UsbDevice usbDevice) {
        return usbDevice.getVendorId() == INTEL_VENDOR_ID
                || usbDevice.getVendorId() == INTEL_RS_VENDOR_ID
                || usbDevice.getVendorId() == MOVIDIUS_VENDOR_ID;
    }

    private static List<UsbDevice> getUsbDevices(Context context, Integer vId) {
        List<UsbDevice> res = null;
        switch (vId) {
            case INTEL_VENDOR_ID:
                res = getUsbDevices(context, INTEL_VENDOR_ID, INTEL_PRODUCT_ID);
                break;
            case INTEL_RS_VENDOR_ID:
                res = getUsbDevices(context, INTEL_RS_VENDOR_ID, INTEL_RS_PRODUCT_ID);
                break;
            case MOVIDIUS_VENDOR_ID:
                res = getUsbDevices(context, MOVIDIUS_VENDOR_ID, MOVIDIUS_PRODUCT_ID);
                break;
        }

        return res;
    }

    private static List<UsbDevice> getUsbDevices(Context context, Integer vId, Integer pId) {
        ArrayList<UsbDevice> res = new ArrayList<>();
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> devicesMap = usbManager.getDeviceList();
        for (Map.Entry<String, UsbDevice> entry : devicesMap.entrySet()) {
            UsbDevice usbDevice = entry.getValue();
            if (usbDevice.getVendorId() == vId && (usbDevice.getProductId() == pId || pId == 0)) {
                res.add(usbDevice);
            }
        }
        if (res.isEmpty())
            Log.w(TAG, "getUsbDevice: failed to locate USB device, " + "VID: " + String.format("0x%04x", vId) + ", PID: " + String.format("0x%04x", pId));
        return res;
    }

    private static boolean hasUsbPermission(Context context, UsbDevice usbDevice){
        Log.d(TAG, "hasUsbPermission");

        if(usbDevice == null){
            Log.w(TAG, "hasUsbPermission: null USB device");
            return false;
        }

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return usbManager.hasPermission(usbDevice);
    }

    private static void grantUsbPermissions(Context context, UsbDevice usbDevice){
        Log.d(TAG, "grantUsbPermissions");

        if(usbDevice == null){
            Log.w(TAG, "grantUsbPermissions: null USB device");
            return;
        }

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        boolean permission = usbManager.hasPermission(usbDevice);

        if(!permission) {
            Log.i(TAG, "grantUsbPermissions:\ndevice: " + usbDevice.toString());
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(UsbUtilities.ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(usbDevice, pi);
        }
    }

    private static List<UsbDevice> getDevices(Context context) {
        List<UsbDevice> res = new ArrayList<>();

        List<UsbDevice> resIntel = getUsbDevices(context, INTEL_VENDOR_ID);
        if(resIntel != null)
            res.addAll(resIntel);

        List<UsbDevice> resIntelRs = getUsbDevices(context, INTEL_RS_VENDOR_ID);
        if(resIntelRs != null)
            res.addAll(resIntelRs);

        List<UsbDevice> resMovidius = getUsbDevices(context, MOVIDIUS_VENDOR_ID);
        if(resMovidius != null)
            res.addAll(resMovidius);

        return res;
    }

    public static void grantUsbPermissionIfNeeded(Context context) {
        List<UsbDevice> usbDevices = getDevices(context);
        for (UsbDevice usbDevice : usbDevices) {
            if (!hasUsbPermission(context, usbDevice)) {
                grantUsbPermissions(context, usbDevice);
            }
        }
    }
}
