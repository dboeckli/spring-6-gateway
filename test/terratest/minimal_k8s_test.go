package terratest

import (
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/gruntwork-io/terratest/modules/k8s"
	"github.com/stretchr/testify/require"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestDeployedPodsAreReady(t *testing.T) {
	namespace := mustEnv(t, "NAMESPACE")
	appName := mustEnv(t, "APPLICATION_NAME")

	kubectlOptions := k8s.NewKubectlOptions("", "", namespace)

	// 1) Namespace erreichbar?
	_, err := k8s.GetNamespaceE(t, kubectlOptions, namespace)
	require.NoError(t, err, "Namespace %q ist nicht erreichbar (kubectl context ok?)", namespace)

	// 2) Auf Pods warten (LabelSelector)
	selector := fmt.Sprintf("app.kubernetes.io/name=%s", appName)

	var lastPods []string

	require.Eventually(t, func() bool {
		pods := k8s.ListPods(t, kubectlOptions, metav1.ListOptions{LabelSelector: selector})
		lastPods = lastPods[:0]
		for _, pod := range pods {
			lastPods = append(lastPods, pod.Name)
		}
		return len(pods) > 0
	}, 2*time.Minute, 3*time.Second, "Keine Pods gefunden mit LabelSelector %q in Namespace %q", selector, namespace)

	// 3) Nur auf "laufende" App-Pods warten (Helm-Test/Job-Pods können Completed sein und werden nie Ready)
	pods := k8s.ListPods(t, kubectlOptions, metav1.ListOptions{LabelSelector: selector})
	require.NotEmpty(t, pods, "Keine Pods gefunden mit LabelSelector %q in Namespace %q", selector, namespace)

	for _, pod := range pods {
		// Helm test Pods / Jobs sind oft Succeeded/Failed und werden nicht Ready
		if pod.Status.Phase == corev1.PodSucceeded || pod.Status.Phase == corev1.PodFailed {
			t.Logf("Überspringe Pod %s (Phase=%s) – vermutlich Helm-Test/Job-Pod", pod.Name, pod.Status.Phase)
			continue
		}

		// Warte darauf, dass der Pod wirklich läuft UND Ready ist
		podName := pod.Name
		require.Eventually(t, func() bool {
			cur, getErr := k8s.GetPodE(t, kubectlOptions, podName)
			if getErr != nil {
				t.Logf("GetPod fehlgeschlagen für %s: %v", podName, getErr)
				return false
			}
			if cur.Status.Phase != corev1.PodRunning {
				t.Logf("Pod %s noch nicht Running (Phase=%s)", podName, cur.Status.Phase)
				return false
			}
			for _, c := range cur.Status.Conditions {
				if c.Type == corev1.PodReady && c.Status == corev1.ConditionTrue {
					return true
				}
			}
			t.Logf("Pod %s ist Running, aber noch nicht Ready", podName)
			return false
		}, 3*time.Minute, 3*time.Second, "Pod %s wurde nicht Ready (Selector=%q, Namespace=%q). Gefundene Pods: %v", podName, selector, namespace, lastPods)
	}
}

func mustEnv(t *testing.T, key string) string {
	t.Helper()
	val := os.Getenv(key)
	require.NotEmpty(t, val, "Environment-Variable %s muss gesetzt sein", key)
	return val
}
