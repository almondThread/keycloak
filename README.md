
Поддержка VK
========

Форк добавляет VK (VKontakte) в список поддерживаемых Social Login провайдеров.


Сборка
========

Нам понадобится сборка только модуля services

```
cd services

mvn clean install -B -DskipTests -Pdistribution
```

Готовый jar файл *keycloak-services-<version>.jar* после сборки должен находиться в services/target
