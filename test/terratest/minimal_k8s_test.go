package terratest

import (
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/gruntwork-io/terratest/modules/k8s"
	"github.com/stretchr/testify/require"
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
		p := k8s.ListPods(t, kubectlOptions, metav1.ListOptions{LabelSelector: selector})
		lastPods = lastPods[:0]
		for _, pod := range p {
			lastPods = append(lastPods, pod.Name)
		}
		return len(p) > 0
	}, 2*time.Minute, 3*time.Second, "Keine Pods gefunden mit LabelSelector %q in Namespace %q", selector, namespace)

	// 3) Auf Ready warten (pro Pod)
	p := k8s.ListPods(t, kubectlOptions, metav1.ListOptions{LabelSelector: selector})
	require.NotEmpty(t, p, "Keine Pods gefunden mit LabelSelector %q in Namespace %q", selector, namespace)

	for _, pod := range p {
		// 60 Retries * 3s = ca. 3 Minuten
		k8s.WaitUntilPodAvailable(t, kubectlOptions, pod.Name, 60, 3*time.Second)
	}
}

func mustEnv(t *testing.T, key string) string {
	t.Helper()
	val := os.Getenv(key)
	require.NotEmpty(t, val, "Environment-Variable %s muss gesetzt sein", key)
	return val
}
