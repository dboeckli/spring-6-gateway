# Kubernetes

## Table of Contents
- [Images](#images)
- [Commands](#commands)
    - [Display resources](#display-resources)
    - [View logs](#view-logs)
    - [Delete all services and deployments](#delete-all-services-and-deployments)
- [Mongo](#mongo)
- [MySQL](#mysql)
- [Auth-server](#auth-server)
- [rest-mvc](#rest-mvc)
- [reactive](#reactive)
- [reactive-mongo](#reactive-mongo)
- [gateway](#gateway)

## Images
* `spring-6-gateway:0.0.1-SNAPSHOT`
* `spring-6-auth-server:0.0.1-SNAPSHOT`
* `spring-6-rest-mvc:0.0.1-SNAPSHOT`
* `spring-6-reactive:0.0.1-SNAPSHOT`
* `reactive-mongo:0.0.1-SNAPSHOT`

![alt text](../docs/guru.png "Overview")

## Commands

### Display resources

Display all K8s resources in the default namespace
```bash
kubectl get all
```

Display all K8s resources in the default namespace with more details
```bash 
kubectl get all -o wide
```

### View logs 

Use parameter -f to follow the logs

```bash 
kubectl logs mongo-<pod-id>
```
```bash 
kubectl logs mysql-<pod-id>
```
```bash 
kubectl logs auth-server-<pod-id>
```
```bash 
kubectl logs rest-mvc-<pod-id>
```
```bash 
kubectl logs reactive-<pod-id>
```
```bash 
kubectl logs reactive-mongo-<pod-id>
```
```bash 
kubectl logs gateway-<pod-id>
```

### Delete all services and deployments
```bash
kubectl delete service mongo
kubectl delete deployment mongo

kubectl delete service mysql
kubectl delete deployment mysql

kubectl delete service auth-server
kubectl delete deployment auth-server

kubectl delete service rest-mvc
kubectl delete deployment rest-mvc

kubectl delete service reactive
kubectl delete deployment reactive

kubectl delete service reactive-mongo
kubectl delete deployment reactive-mongo

kubectl delete service gateway
kubectl delete deployment gateway
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
Create Service for rest-mvc
```bash
kubectl create service clusterip rest-mvc --tcp=8080:8080 --dry-run=client -o yaml > rest-mvc-service.yaml
```

Apply Service for rest-mvc
```bash
kubectl apply -f rest-mvc-service.yaml
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
kubectl create service clusterip reactive --tcp=8080:8080 --dry-run=client -o yaml > reactive-service.yaml
```

Apply Service for reactive
```bash
kubectl apply -f reactive-service.yaml
```

## reactive-mongo

Create Deployment for reactive-mongo
```bash
kubectl create deployment reactive-mongo --image=spring-6-reactive-mongo:0.0.1-SNAPSHOT --dry-run=client -o yaml > reactive-mongo-deployment.yaml
```

Apply Deployment for reactive-mongo
```bash
kubectl apply -f reactive-mongo-deployment.yaml
```

Create Service for reactive-mongo
```bash
kubectl create service clusterip reactive-mongo --tcp=8080:8080 --dry-run=client -o yaml > reactive-mongo-service.yaml
```

Apply Service for reactive-mongo
```bash
kubectl apply -f reactive-mongo-service.yaml
```

## gateway

Create Deployment for gateway
```bash
kubectl create deployment gateway --image=spring-6-gateway:0.0.1-SNAPSHOT --dry-run=client -o yaml > gateway-deployment.yaml
```

Apply Deployment for gateway
```bash
kubectl apply -f gateway-deployment.yaml
```

Create Service for gateway
```bash
kubectl create service clusterip gateway --tcp=8080:8080 --dry-run=client -o yaml > gateway-service.yaml
```

Apply Service for gateway
```bash
kubectl apply -f gateway-service.yaml
```

## Expose port 

### Port forward to Gateway
This is exposing the gateway to the outside world on the port 8080
TODO: This is just a workaround
```bash
kubectl port-forward service/gateway 8080:8080
```

Instead of starting port-forwarding we can do the same with ingress

### Ingress

Install Ingress Controller
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
```

And wait for installation
```bash
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=120s
```

The Ingress Controller listen on port 80 and 443 by default
Check:
```bash
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx 
```

Now we change the port 80 to 8080
```bash
kubectl edit svc ingress-nginx-controller -n ingress-nginx
```
Now we can create a new file 'ingress-nginx-controller.yaml' and paste the content there
In the file we change the port 80 to 8080 and save the file.

Deploy changed Ingress Controller
```bash
kubectl apply -f ingress-nginx-controller.yaml
```

Check again if ingress controller is listening on port 8080:
```bash
kubectl get svc -n ingress-nginx
```

Finally we create an ingress for our gateway
```bash
kubectl create ingress gateway-ingress --rule="/*=gateway:8080" --class=nginx --annotation="kubernetes.io/ingress.class=nginx" --dry-run=client -o yaml > gateway-ingress.yaml
```  

Apply Service for gateway
```bash
kubectl apply -f gateway-ingress.yaml
``` 

Check ingress
```bash
kubectl get ingress
``` 
