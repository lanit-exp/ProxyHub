# RemoteHub

## Регистрация узлов.

Для регистрации узлов необходимо отправить запрос по адресу http://host:port/register/node
(например http://localhost:4444/api/register/node) JSON с основными параметрами ноды.
Пример JSON:

```
{
    "host" : "host",
    "port" : "port",
    "applicationName" : "applicationName"
}
```
, где параметр "applicationName" можно опустить, т.к. в таком случае хаб сам сгенерирует имя узла.

Получить информацию о всех узлах можно, отправив GET-запрос по адресу 
http://host:port/get/nodes (например http://localhost:4444/status).

## Swagger

В случае успешного запуска программы по адресу 

    http://localhost:4444/swagger-ui.html

будет доступен Swagger.