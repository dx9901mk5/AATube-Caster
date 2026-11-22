package com.aayt.caster.shizuku

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

/**
 * Pure Kotlin Binder IPC interface for Shizuku UserService (equivalent to AIDL,
 * avoids Windows aidl.exe non-ASCII path encoding issues).
 */
interface IPrivilegedAaService : IInterface {

    fun destroy()
    fun runAaRecognitionScript(
        appPackage: String,
        sourceApkPath: String,
        forceReinstallPlayStore: Boolean
    ): String

    fun installApkAsPlayStore(apkPath: String): Boolean

    abstract class Stub : Binder(), IPrivilegedAaService {

        init {
            attachInterface(this, DESCRIPTOR)
        }

        override fun asBinder(): IBinder = this

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            when (code) {
                INTERFACE_TRANSACTION -> {
                    reply?.writeString(DESCRIPTOR)
                    return true
                }
                TRANSACTION_DESTROY -> {
                    data.enforceInterface(DESCRIPTOR)
                    destroy()
                    return true
                }
                TRANSACTION_RUN_AA_SCRIPT -> {
                    data.enforceInterface(DESCRIPTOR)
                    val pkg = data.readString().orEmpty()
                    val apk = data.readString().orEmpty()
                    val force = data.readInt() != 0
                    val result = runAaRecognitionScript(pkg, apk, force)
                    reply?.writeNoException()
                    reply?.writeString(result)
                    return true
                }
                TRANSACTION_INSTALL_APK -> {
                    data.enforceInterface(DESCRIPTOR)
                    val apk = data.readString().orEmpty()
                    val ok = installApkAsPlayStore(apk)
                    reply?.writeNoException()
                    reply?.writeInt(if (ok) 1 else 0)
                    return true
                }
            }
            return super.onTransact(code, data, reply, flags)
        }

        private class Proxy(private val remote: IBinder) : IPrivilegedAaService {
            override fun asBinder(): IBinder = remote

            override fun destroy() {
                val data = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    remote.transact(TRANSACTION_DESTROY, data, null, FLAG_ONEWAY)
                } finally {
                    data.recycle()
                }
            }

            override fun runAaRecognitionScript(
                appPackage: String,
                sourceApkPath: String,
                forceReinstallPlayStore: Boolean
            ): String {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeString(appPackage)
                    data.writeString(sourceApkPath)
                    data.writeInt(if (forceReinstallPlayStore) 1 else 0)
                    remote.transact(TRANSACTION_RUN_AA_SCRIPT, data, reply, 0)
                    reply.readException()
                    reply.readString().orEmpty()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }

            override fun installApkAsPlayStore(apkPath: String): Boolean {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeString(apkPath)
                    remote.transact(TRANSACTION_INSTALL_APK, data, reply, 0)
                    reply.readException()
                    reply.readInt() != 0
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }
        }

        companion object {
            private const val DESCRIPTOR = "com.aayt.caster.shizuku.IPrivilegedAaService"
            private const val TRANSACTION_DESTROY = FIRST_CALL_TRANSACTION + 0
            private const val TRANSACTION_RUN_AA_SCRIPT = FIRST_CALL_TRANSACTION + 1
            private const val TRANSACTION_INSTALL_APK = FIRST_CALL_TRANSACTION + 2

            fun asInterface(obj: IBinder?): IPrivilegedAaService? {
                if (obj == null) return null
                val iin = obj.queryLocalInterface(DESCRIPTOR)
                return if (iin != null && iin is IPrivilegedAaService) {
                    iin
                } else {
                    Proxy(obj)
                }
            }
        }
    }
}
