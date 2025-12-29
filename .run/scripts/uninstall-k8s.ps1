cd target/helm/repo

$file = Get-ChildItem -Filter spring-6-gateway-v*.tgz | Select-Object -First 1
$APPLICATION_NAME = Get-ChildItem -Directory | Where-Object { $_.LastWriteTime -ge $file.LastWriteTime } | Select-Object -ExpandProperty Name

helm uninstall $APPLICATION_NAME --namespace spring-6-gateway

kubectl delete pod -n spring-6-gatewayc --field-selector=status.phase==Succeeded
kubectl delete pod -n spring-6-gateway --field-selector=status.phase==Failed
