package tornadofx.di.frameworks.spring

import org.springframework.beans.factory.BeanFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import tornadofx.*

class SpringController(vararg xmlBeanFactory: String): Controller() {
    val appContext = ClassPathXmlApplicationContext(*xmlBeanFactory) as BeanFactory
}