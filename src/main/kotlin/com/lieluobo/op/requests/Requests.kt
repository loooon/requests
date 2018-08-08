package com.lieluobo.op.requests

import okhttp3.*
import okio.ByteString
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class Requests private constructor(private val client: OkHttpClient, url: HttpUrl) {
    private val requestBuilder = Request.Builder()
    private val urlBuilder = url.newBuilder()
    private var body: RequestBody = RequestBody.create(null, ByteString.EMPTY)

    companion object {
        private const val DEFAULT_CONNECT_TIMEOUT = 3000L
        private const val DEFAULT_READ_TIMEOUT = 3000L
        private const val DEFAULT_WRITE_TIMEOUT = 3000L

        @JvmStatic
        private val builder = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)

        @JvmStatic
        private var client: OkHttpClient? = null


        /**
         * 配置超时参数，超时参数全局生效， 单位毫秒
         * @param connect 连接超时时间，默认3秒
         * @param read 接收超时时间， 默认3秒
         * @param write 发送超时时间，默认3秒
         */
        @JvmStatic
        fun timeout(connect: Long = DEFAULT_CONNECT_TIMEOUT, read: Long = DEFAULT_READ_TIMEOUT, write: Long = DEFAULT_WRITE_TIMEOUT) {
            builder.connectTimeout(connect, TimeUnit.MILLISECONDS)
                    .readTimeout(read, TimeUnit.MILLISECONDS)
                    .writeTimeout(write, TimeUnit.MILLISECONDS)
        }

        /**
         * 根据url构建 {@link Requests 对象}
         * @param url 请求的url
         * @return Requests
         */
        @JvmStatic
        fun url(url: HttpUrl): Requests {
            synchronized(this) {
                if (client == null) {
                    client = builder.build()
                }
            }
            return Requests(client!!, url)
        }

        /**
         * 根据url构建 {@link Requests 对象}
         * @param url 请求的url
         * @return Requests
         */
        @JvmStatic
        fun url(url: String): Requests {
            return this.url(HttpUrl.get(url)!!)
        }

        /**
         * 根据url构建 {@link Requests 对象}
         * @param url 请求的url
         * @return Requests
         */
        @JvmStatic
        fun url(uri: URI): Requests {
            return this.url(HttpUrl.get(uri)!!)
        }

        /**
         * 根据url构建 {@link Requests 对象}
         * @param url 请求的url
         * @return Requests
         */
        @JvmStatic
        fun url(url: URL): Requests {
            return this.url(HttpUrl.get(url)!!)
        }
    }

    private fun execute(request: Request): Response {
        return client.newCall(request).execute()
    }

    /**
     * 设置请求头，用于header name 存在重复的情况
     * @param headers
     * @return Requests
     */
    fun headers(headers: List<Pair<String, String>>): Requests {
        headers.forEach {
            requestBuilder.addHeader(it.first, it.second)
        }
        return this
    }

    /**
     * 设置请求头，用于header name 不重复的情况
     * @param headers
     * @return Requests
     */
    fun headers(headers: Map<String, String>): Requests {
        return this.headers(headers.toList())
    }

    /**
     * 设置请求url参数，用于参数名称存在重复的情况
     * @param params
     * @return Requests
     */
    fun params(params: List<Pair<String, String>>): Requests {
        params.forEach {
            urlBuilder.addQueryParameter(it.first, it.second)
        }
        return this
    }

    /**
     * 设置请求url参数，用于参数名称不重复的情况
     * @param params
     * @return Requests
     */
    fun params(params: Map<String, String>): Requests {
        return this.params(params.toList())
    }

    /**
     * 设置请求form data，用于名称重复的情况
     * @param data form data
     * @param charset 编码方式， 默认使用系统默认编码
     * @return Requests
     */
    fun form(data: List<Pair<String, String>>, charset: Charset = Charset.defaultCharset()): Requests {
        val builder = FormBody.Builder(charset)
        data.forEach {
            builder.add(it.first, it.second)
        }
        body = builder.build()
        return this
    }

    /**
     * 设置请求form data，用于名称不重复的情况
     * @param data form data
     * @param charset 编码方式， 默认使用系统默认编码
     * @return Requests
     */
    fun form(data: Map<String, String>, charset: Charset = Charset.defaultCharset()): Requests {
        return form(data.toList(), charset)
    }

    /**
     * body为json时使用
     * @param json form data
     * @param charset 编码方式， 默认使用系统默认编码
     * @return Requests
     */
    fun json(json: String, charset: Charset = Charset.defaultCharset()): Requests {
        body = RequestBody.create(MediaType.parse("application/json; charset=${charset.name()}"), json)
        return this
    }

    /**
     * 文件上传
     * @param file 要上传的文件
     * @param formData 附加的form data
     * @return Requests
     */
    fun file(file: File, formData: List<Pair<String, String>> = listOf()): Requests {
        val type = Files.probeContentType(Paths.get(file.absolutePath)) ?: "application/octet-stream"
        val fileBody = RequestBody.create(MediaType.parse(type), file)
        val builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(type, file.name, fileBody)
        formData.forEach {
            builder.addFormDataPart(it.first, it.second)
        }
        body = builder.build()
        return this
    }

    /**
     * body可以是任意二进制
     * @param data request body
     * @param mediaType content-type 默认值 application/octet-stream
     * @return Requests
     */
    fun binary(data: ByteArray, mediaType: String = "application/octet-stream"): Requests {
        body = RequestBody.create(MediaType.parse(mediaType), data)
        return this
    }

    /**
     * get 请求， {@link Response}对象使用完毕后需要close
     * @see Response#close
     * @return Response
     */
    fun get(): Response {
        val request = requestBuilder
                .url(urlBuilder.build())
                .get()
                .build()
        return execute(request)
    }

    /**
     * get 请求， 使用lambda表达式处理Response
     * @param lambda 处理Response的lambda表达式
     * @return T
     */
    fun <T> get(lambda: (Response) -> T): T {
        val response = get()
        return response.use(lambda)
    }

    /**
     * post 请求， {@link Response}对象使用完毕后需要close
     * @see Response#close
     * @return Response
     */
    fun post(): Response {
        val request = requestBuilder.url(urlBuilder.build())
                .post(body)
                .build()
        return execute(request)
    }

    /**
     * post 请求， 使用lambda表达式处理Response
     * @param lambda 处理Response的lambda表达式
     * @return T
     */
    fun <T> post(lambda: (Response) -> T): T {
        return post().use(lambda)
    }

    /**
     * put 请求， {@link Response}对象使用完毕后需要close
     * @see Response#close
     * @return Response
     */
    fun put(): Response {
        val request = requestBuilder.url(urlBuilder.build())
                .put(body)
                .build()
        return execute(request)
    }

    /**
     * put 请求， 使用lambda表达式处理Response
     * @param lambda 处理Response的lambda表达式
     * @return T
     */
    fun <T> put(lambda: (Response) -> T): T {
        return put().use(lambda)
    }

    /**
     * delete 请求， {@link Response}对象使用完毕后需要close
     * @see Response#close
     * @return Response
     */
    fun delete(): Response {
        val request = requestBuilder.url(urlBuilder.build())
                .delete(body)
                .build()
        return execute(request)
    }

    /**
     * delete 请求， 使用lambda表达式处理Response
     * @param lambda 处理Response的lambda表达式
     * @return T
     */
    fun <T> delete(lambda: (Response) -> T): T {
        return delete().use(lambda)
    }

    /**
     * patch 请求， {@link Response}对象使用完毕后需要close
     * @see Response#close
     * @return Response
     */
    fun patch(): Response {
        val request = requestBuilder.url(urlBuilder.build())
                .patch(body)
                .build()
        return execute(request)
    }

    /**
     * patch 请求， 使用lambda表达式处理Response
     * @param lambda 处理Response的lambda表达式
     * @return T
     */
    fun <T> patch(lambda: (Response) -> T): T {
        return patch().use(lambda)
    }
}