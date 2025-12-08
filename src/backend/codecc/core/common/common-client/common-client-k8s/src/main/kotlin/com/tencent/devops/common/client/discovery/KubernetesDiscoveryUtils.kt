package com.tencent.devops.common.client.discovery

import com.tencent.devops.common.service.Profile
import io.kubernetes.client.openapi.apis.CoreV1Api
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.kubernetes.client.KubernetesClientPodUtils
import java.net.InetAddress

class KubernetesDiscoveryUtils constructor(
    private val namespace: String,
    private val coreV1Api: CoreV1Api,
    private val kubernetesClientPodUtils: KubernetesClientPodUtils,
    private val discoveryClient: DiscoveryClient,
    private val profile: Profile
) : DiscoveryUtils {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesDiscoveryUtils::class.java)
    }

    override fun getInstanceTags(instance: ServiceInstance): List<String> {
        val pods = coreV1Api.listNamespacedPod(
            namespace, null, null, null,
            null, null, null, null, null,
            null, null, null
        )
        for (v1Pod in pods.items) {
            if (v1Pod.status?.podIP == instance.host) {
                return listOf(v1Pod.metadata?.name ?: instance.instanceId)
            }
        }
        return listOf(instance.instanceId)
    }

    override fun getRegistrationTags(instance: ServiceInstance): List<String> {
        return listOf(kubernetesClientPodUtils.currentPod().get().metadata?.name ?: instance.instanceId)
    }

    override fun getRegistration(): ServiceInstance {
        val instances = discoveryClient.getInstances(profile.getServiceName()) ?: emptyList()
        val ip = InetAddress.getLocalHost().hostAddress
        val localInstance = instances.firstOrNull { instance -> instance.host == ip }
        return localInstance!!
    }
}