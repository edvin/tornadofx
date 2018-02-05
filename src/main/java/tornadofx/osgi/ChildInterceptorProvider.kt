package tornadofx.osgi

import org.osgi.framework.BundleContext
import tornadofx.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

interface ChildInterceptorProvider {
    val interceptor: ChildInterceptor
}

inline fun <reified T : ChildInterceptor> BundleContext.registerChildInterceptor() = registerChildInterceptor(T::class)
fun BundleContext.registerChildInterceptor(interceptorKlass: KClass<out ChildInterceptor>) {
    val provider = object : ChildInterceptorProvider {
        override val interceptor = interceptorKlass.createInstance()
    }
    registerService(ChildInterceptorProvider::class.java, provider, Hashtable<String, String>())
}