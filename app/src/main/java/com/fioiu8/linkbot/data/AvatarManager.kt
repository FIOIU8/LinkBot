package com.fioiu8.linkbot.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * 头像管理器 - 负责头像的保存、读取和删除
 *
 * @param context Android 上下文
 */
class AvatarManager(private val context: Context) {

    /**
     * 头像存储目录
     */
    private val avatarDir: File
        get() = File(context.filesDir, "avatars").also { it.mkdirs() }

    /**
     * 保存头像
     * @param uri 头像文件 URI
     * @param type 头像类型（user/ai）
     * @return 保存后的文件路径，如果保存失败返回 null
     */
    fun saveAvatar(uri: Uri, type: String): String? {
        return try {
            // 从 URI 获取输入流并解码为 Bitmap
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // 将图片缩放为 200x200
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

            // 保存到文件
            val file = File(avatarDir, "avatar_$type.png")
            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // 释放原始 bitmap 资源
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取头像路径
     * @param type 头像类型（user/ai）
     * @return 头像文件路径，如果不存在返回 null
     */
    fun getAvatarPath(type: String): String? {
        val file = File(avatarDir, "avatar_$type.png")
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * 删除头像
     * @param type 头像类型（user/ai）
     */
    fun deleteAvatar(type: String) {
        File(avatarDir, "avatar_$type.png").delete()
    }

    /**
     * 检查是否存在头像
     * @param type 头像类型（user/ai）
     * @return 是否存在头像
     */
    fun hasAvatar(type: String): Boolean {
        return File(avatarDir, "avatar_$type.png").exists()
    }
}