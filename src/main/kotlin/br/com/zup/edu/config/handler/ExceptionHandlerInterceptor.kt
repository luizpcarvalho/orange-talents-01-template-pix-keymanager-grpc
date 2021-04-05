package br.com.zup.edu.config.handler

import io.grpc.BindableService
import io.grpc.stub.StreamObserver
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionHandlerInterceptor(@Inject private val resolver: ExceptionHandlerResolver)
    : MethodInterceptor<Any, Any?> {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun intercept(context: MethodInvocationContext<Any, Any?>): Any? {
        return try{
            context.proceed()
        } catch (e: Exception) {
            logger.error("Handling the exception '${e.javaClass.name}' while processing the call: ${context.targetMethod}", e)

            val handler = resolver.resolve(e)
            val status = handler.handle(e)
            val observer = context.parameterValues[1] as StreamObserver<*>
            observer.onError(status.asRuntimeException())

            null
        }
    }

}