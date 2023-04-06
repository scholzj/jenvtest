package io.javaoperatorsdk.jenvtest.junit;

import java.util.Map;

import org.assertj.core.api.Assertions;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

public class TestUtils {

  public static ConfigMap testConfigMap() {
    return new ConfigMapBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("test1")
            .withNamespace("default")
            .build())
        .withData(Map.of("key", "data"))
        .build();
  }

  public static void simpleTest() {
    simpleTest(new KubernetesClientBuilder().build());
  }

  public static void simpleTest(KubernetesClient client) {
    client.resource(TestUtils.testConfigMap()).create();
    var cm = client.resource(TestUtils.testConfigMap()).get();

    Assertions.assertThat(cm).isNotNull();

    client.resource(cm).delete();
  }

}
