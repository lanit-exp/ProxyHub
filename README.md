# RemoteHub

## Регистрация узлов

Для регистрации узлов необходимо отправить запрос по адресу http://host:port/register/node
(например http://localhost:4444/api/register/node) с JSON, в котором содержатся основные параметры ноды.
Пример JSON:

```
{
    "host" : "host",
    "port" : "port",
    "applicationName" : "applicationName"
}
```
, где параметр "applicationName" можно опустить, т.к. в таком случае хаб сам сгенерирует имя узла. 
В качестве альтеранативы, можно зарегистрировать узлы при помощи файла nodes.yaml. Однако, в таком случае, 
необходимо обязательно задать имя узла, иначе, это приведет к ошибке чтения файла. При запуске jar-файла с сервисом 
обязательно необходимо, чтобы данный файл находился в той же директории, что и само приложение.

Получить информацию о всех узлах можно, отправив GET-запрос по адресу 
http://host:port/status (например http://localhost:4444/status).

## Управление узлами
При необходимости запуска тестов на определенном узле, необходимо добавить в capabilities веб драйвера параметр 
"applicationName", указав в нем его имя.

## Отправка запроса напрямую в веб драйвер.
В хабе присутствует возможность напрямую отправлять запросы в веб драйвер без использования прокси драйвера. Для
этого необходимо зарегистрировать узел, указав адрес, порт и, обязательно, имя узла. После этого, добавляем в capabilities веб драйвера параметр 
"applicationName", указав в нем его имя.

## Swagger

В случае успешного запуска программы по адресу 

    http://localhost:4444/swagger-ui.html

будет доступен Swagger.

## API
* ```POST: /api/register/node```
* ```GET: /api/delete/nodes```
* ```GET: /api/delete/node/{name}```
* ```GET: /api/set/timeout/{value}```