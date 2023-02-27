import com.tencent.devops.utils.findPropertyOrEmpty
import com.tencent.devops.enums.AssemblyMode

plugins {
    id("com.tencent.devops.boot")
}

val property = project.findPropertyOrEmpty("devops.assemblyMode").trim()

println("gradle assemly mode property is $property")

fun getImportByProperty(property: String) : AssemblyMode{
    return when (val assemblyMode = AssemblyMode.ofValueOrDefault(property)) {
        AssemblyMode.CONSUL,AssemblyMode.K8S -> assemblyMode
        else -> AssemblyMode.CONSUL
    }
}

project.dependencies.add("api", project(":core:common:common-client:common-client-${getImportByProperty(property).name.toLowerCase()}"))
