/*
 * StarDict implementation for Android.
 * Copyright (C) 2022  Bohdan Kolvakh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.acmpo6ou.stardict.dicts_screen

import android.content.ClipData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.acmpo6ou.stardict.MyApp
import com.acmpo6ou.stardict.utils.StarDict
import com.vanpra.composematerialdialogs.MaterialDialogState
import java.io.File

open class DictsViewModel : ViewModel() {
    lateinit var app: MyApp
    val dicts = MutableLiveData<List<StarDict>>(listOf())

    val error = MutableLiveData("")
    lateinit var errorDialog: MaterialDialogState
    var dictToRemove: MutableLiveData<StarDict?> = MutableLiveData(null)

    private fun addDict(dict: StarDict) {
        if (dict.filePath in dicts.value!!.map { it.filePath }) return

        val list = dicts.value!!.toMutableList()
        list.add(dict)
        dicts.value = list.toList()
    }

    private fun removeDict(dict: StarDict) {
        val list = dicts.value!!.toMutableList()
        list.remove(dict)
        dicts.value = list.toList()
    }

    /**
     * Loads dictionary handling all errors, deletes all files of a dict
     * from SRC_DIR if there is an error.
     */
    fun loadDictionary(name: String) {
        try {
            val dict = StarDict()
            dict.initialize("${app.SRC_DIR}/$name")
            addDict(dict)
        } catch (e: Exception) {
            e.printStackTrace()
            error.value = e.toString()
            errorDialog.show()

            // delete all dict files since they are probably invalid
            for (ext in listOf("ifo", "idx", "dict"))
                File("${app.SRC_DIR}/$name.$ext").delete()
        }
    }

    /**
     * Populates [dicts] with dictionaries residing in the SRC_DIR.
     */
    fun loadDicts() {
        for (file in File(app.SRC_DIR).listFiles()) {
            if (file.extension != "ifo") continue
            loadDictionary(file.nameWithoutExtension)
        }
    }

    /**
     * Copies dict files from [data] to SRC_DIR.
     * @param data contains URIs with paths to dict files.
     */
    open fun copyDictFiles(data: ClipData) {
        for (i in 0 until data.itemCount) {
            val uri = data.getItemAt(i).uri
            val ext = File(uri.path!!).extension

            // skip all non dict files
            if (ext !in listOf("idx", "ifo", "dict"))
                continue

            val name = File(uri.path!!).name
            val file = File("${app.SRC_DIR}/$name")
            val stream = app.contentResolver.openInputStream(uri)

            stream.use {
                file.writeBytes(it!!.readBytes())
            }
        }
    }

    /**
     * Copies dict files using [copyDictFiles] handling all errors.
     */
    fun importDict(data: ClipData) {
        try {
            copyDictFiles(data)
        } catch (e: Exception) {
            e.printStackTrace()
            error.value = e.toString()
            errorDialog.show()
            return
        }

        val path = data.getItemAt(0).uri.path!!
        val name = File(path).nameWithoutExtension
        loadDictionary(name)
    }

    /**
     * Deletes dict files and removes the dict from dicts list.
     */
    fun deleteDict() {
        val dict = dictToRemove.value!!
        for (ext in listOf("ifo", "idx", "dict")) {
            val file = File("${dict.filePath}.$ext")
            file.delete()
        }
        removeDict(dict)
    }
}
