package io.javaoperatorsdk.jenvtest.kubeconfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.jenvtest.JenvtestException;
import io.javaoperatorsdk.jenvtest.binary.BinaryManager;
import io.javaoperatorsdk.jenvtest.cert.CertManager;

public class KubeConfig {

  private static final Logger log = LoggerFactory.getLogger(KubeConfig.class);
  public static final String JENVTEST = "jenvtest";

  private final CertManager certManager;
  private final BinaryManager binaryManager;

  public KubeConfig(CertManager certManager, BinaryManager binaryManager) {
    this.certManager = certManager;
    this.binaryManager = binaryManager;
  }

  public void updateKubeConfig(int apiServerPort) {
    log.debug("Updating kubeconfig");
    execWithKubectlConfigAndWait("set-cluster", JENVTEST,
        "--server=https://127.0.0.1:" + apiServerPort,
        "--certificate-authority=" + certManager.getAPIServerCertPath());
    execWithKubectlConfigAndWait("set-credentials", JENVTEST,
        "--client-certificate=" + certManager.getClientCertPath(),
        "--client-key=" + certManager.getClientKeyPath());
    execWithKubectlConfigAndWait("set-context", JENVTEST, "--cluster=jenvtest",
        "--namespace=default", "--user=jenvtest");
    execWithKubectlConfigAndWait("use-context", JENVTEST);
  }

  public void cleanupFromKubeConfig() {
    log.debug("Cleanig up kubeconfig");
    unset("contexts." + JENVTEST);
    unset("clusters." + JENVTEST);
    unset("users." + JENVTEST);
    unset("current-context");
  }

  private void unset(String target) {
    execWithKubectlConfigAndWait("unset", target);
  }

  public String generateKubeConfigYaml(int apiServerPort) {
    try (InputStream is = KubeConfig.class.getResourceAsStream("/kubeconfig-template.yaml")) {
      String template = IOUtils.toString(is, StandardCharsets.UTF_8);
      Object[] args = new Object[] {certManager.getAPIServerCertPath(),
          apiServerPort, certManager.getClientCertPath(), certManager.getClientKeyPath()};
      MessageFormat format = new MessageFormat(template);
      return format.format(args);
    } catch (IOException e) {
      throw new JenvtestException(e);
    }
  }

  private void execWithKubectlConfigAndWait(String... arguments) {
    try {
      List<String> args = new ArrayList<>(arguments.length + 2);
      args.add(binaryManager.binaries().getKubectl().getPath());
      args.add("config");
      args.addAll(List.of(arguments));
      var process = new ProcessBuilder(args).start();
      process.waitFor();
    } catch (IOException e) {
      throw new JenvtestException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JenvtestException(e);
    }
  }
}