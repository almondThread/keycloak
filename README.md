
Поддержка VK
========

Форк добавляет VK (VKontakte) в список поддерживаемых Social Login провайдеров.


Сборка
========

Нам понадобится только сборка  модуля services

```
cd services

mvn clean install -B -DskipTests -Pdistribution
```

Готовый jar файл *keycloak-services-4.0.0.beta3.jar* после сборки должен находиться в services/target
