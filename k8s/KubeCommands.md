# Images
* `spring-6-gateway:0.0.1-SNAPSHOT`
* `spring-6-auth-server:0.0.1-SNAPSHOT`
* `spring-6-rest-mvc:0.0.1-SNAPSHOT`
* `spring-6-reactive:0.0.1-SNAPSHOT`
* `reactive-mongo:0.0.1-SNAPSHOT`

Display all K8s resources in the default namespace
```bash
kubectl get all
```

Display all K8s resources in the default namespace with more details
```bash 
kubectl get all -o wide
```

## Mongo
Create Deployment for Mongo
```bash
kubectl create deployment mongo --image=mongo --dry-run=client -o yaml > mongo-deployment.yaml
```

Apply Deployment
```bash
kubectl apply -f mongo-deployment.yaml
```

Create Service for Mongo
```bash
kubectl create service clusterip mongo --tcp=27017:27017 --dry-run=client -o yaml > mongo-service.yaml
```

Apply Service for Mongo
```bash
kubectl apply -f mongo-service.yaml
```

Delete Service for Mongo
```bash
kubectl delete service mongo
```

Delete Deployment for Mongo
```bash
kubectl delete deployment mongo
```

View logs for Mongo
```bash 
kubectl logs mongo-<pod-id>
```
## Mysql

Create Deployment for Mysql
```bash
kubectl create deployment mysql --image=mysql:9 --dry-run=client -o yaml > mysql-deployment.yaml
```

Apply Deployment
```bash
kubectl apply -f mysql-deployment.yaml
```

Create Service for Mysql
```bash
kubectl create service clusterip mysql --tcp=3306:3306 --dry-run=client -o yaml > mysql-service.yaml
```

Apply Service for Mysql
```bash
kubectl apply -f mysql-service.yaml
```

## Auth-server

Create Deployment for Auth-server
```bash
kubectl create deployment auth-server --image=spring-6-auth-server:0.0.1-SNAPSHOT --dry-run=client -o yaml > auth-server-deployment.yaml
```

Apply Deployment for Auth-server
```bash
kubectl apply -f auth-server-deployment.yaml
```

Create Service for Auth-server
```bash
kubectl create service clusterip auth-server --tcp=9000:9000 --dry-run=client -o yaml > auth-server-service.yaml
```

Apply Service for Auth-server
```bash
kubectl apply -f auth-server-service.yaml
```

## rest-mvc

Create Deployment for rest-mvc
```bash
kubectl create deployment rest-mvc --image=spring-6-rest-mvc:0.0.1-SNAPSHOT --dry-run=client -o yaml > rest-mvc-deployment.yaml
```

Apply Deployment for rest-mvc
```bash
kubectl apply -f rest-mvc-deployment.yaml
```
rest-mvc
Create Service for rest-mvc
```bash
kubectl create service clusterip rest-mvc --tcp=8081:8080 --dry-run=client -o yaml > rest-mvc-service.yaml
```

## reactive

Create Deployment for reactive
```bash
kubectl create deployment reactive --image=spring-6-reactive:0.0.1-SNAPSHOT --dry-run=client -o yaml > reactive-deployment.yaml
```

Apply Deployment for reactive
```bash
kubectl apply -f reactive-deployment.yaml
```
reactive
Create Service for reactive
```bash
kubectl create service clusterip reactive --tcp=8082:8080 --dry-run=client -o yaml > reactive-service.yaml
```

Apply Service for reactive
```bash
kubectl apply -f reactive-service.yaml
```


## Port forward to Gateway
```bash
kubectl port-forward service/gateway 8080:8080
```
