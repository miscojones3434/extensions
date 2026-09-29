package com.patriarca.movistarplus

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@CloudstreamPlugin
class MovistarPlusPlugin : Plugin() {

    companion object {
        private const val PREFS_NAME = "movistarplus_credentials"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }

    private val pluginScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        openSettings = { context ->
            showLoginDialog(context)
        }
    }

    override fun load(context: Context) {
        registerMainAPI(MovistarPlusProvider())

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val username = prefs
            .getString(KEY_USERNAME, "")
            .orEmpty()

        val password = prefs
            .getString(KEY_PASSWORD, "")
            .orEmpty()

        if (username.isNotBlank() && password.isNotBlank()) {
            pluginScope.launch {
                connect(
                    username = username,
                    password = password
                )
            }
        }
    }

    private fun showLoginDialog(context: Context) {
        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val density = context.resources.displayMetrics.density
        val padding = (24 * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                padding,
                padding / 2,
                padding,
                0
            )
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val usernameField = EditText(context).apply {
            hint = "Usuario / correo de Movistar Plus+"
            setSingleLine(true)
            inputType =
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

            setText(
                prefs.getString(
                    KEY_USERNAME,
                    ""
                ).orEmpty()
            )
        }

        val passwordField = EditText(context).apply {
            hint = "Contraseña"
            setSingleLine(true)
            inputType =
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD

            setText(
                prefs.getString(
                    KEY_PASSWORD,
                    ""
                ).orEmpty()
            )
        }

        container.addView(usernameField)
        container.addView(passwordField)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Movistar Plus+")
            .setMessage(
                "Introduce los datos de tu cuenta para cargar canales y contenido."
            )
            .setView(container)
            .setPositiveButton(
                "Guardar e iniciar sesión",
                null
            )
            .setNeutralButton(
                "Cerrar sesión",
                null
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val username =
                        usernameField.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()

                    val password =
                        passwordField.text
                            ?.toString()
                            .orEmpty()

                    if (
                        username.isBlank() ||
                        password.isBlank()
                    ) {
                        Toast.makeText(
                            context,
                            "Introduce usuario y contraseña",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    prefs.edit()
                        .putString(
                            KEY_USERNAME,
                            username
                        )
                        .putString(
                            KEY_PASSWORD,
                            password
                        )
                        .apply()

                    dialog.dismiss()

                    Toast.makeText(
                        context,
                        "Conectando con Movistar Plus+…",
                        Toast.LENGTH_SHORT
                    ).show()

                    pluginScope.launch {
                        val connected = connect(
                            username = username,
                            password = password
                        )

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (connected) {
                                    "Movistar Plus+ conectado"
                                } else {
                                    "No se pudo iniciar sesión"
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            dialog
                .getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener {

                    prefs.edit()
                        .remove(KEY_USERNAME)
                        .remove(KEY_PASSWORD)
                        .apply()

                    MovistarAuthClient.logout()

                    dialog.dismiss()

                    Toast.makeText(
                        context,
                        "Sesión de Movistar Plus+ cerrada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        dialog.show()
    }

    private suspend fun connect(
        username: String,
        password: String
    ): Boolean {

        MovistarSessionManager.clear()

        val authenticated =
            try {
                MovistarAuthClient.login(
                    username = username,
                    password = password
                )
            } catch (_: Throwable) {
                false
            }

        if (!authenticated) {
            return false
        }

        return try {
            MovistarDeviceClient.prepareDevice()
        } catch (_: Throwable) {
            false
        }
    }
}
