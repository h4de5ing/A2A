/*
 * Copyright (C) 2020 Orange
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.devicefarmer.minicap

import io.devicefarmer.minicap.utils.Ln
import java.io.IOException
import java.net.Socket

/**
 * Minimalist "server" to bootstrap development
 */
class SimpleServer(private val socket: Socket, private val listener: Listener) {
    interface Listener {
        fun onConnection(socket: Socket)
    }

    fun start() {
        try {
            listener.onConnection(socket)
        } catch (e: IOException) {
            Ln.e("error waiting connection", e)
        }
    }
}
