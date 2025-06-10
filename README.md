# Detección de Monedas y Rostros con OpenCV en Android

Este proyecto es una aplicación Android que aprovecha la potencia de la librería **OpenCV** para realizar tareas de visión por computadora en tiempo real utilizando la cámara del dispositivo. La aplicación ofrece dos modos principales de funcionamiento:

* Detección y suma del valor de monedas.
* Detección de rostros.

---

## Funcionalidades Principales

### 🪙 Detección de Monedas

* Utiliza la **Transformada de Hough para Círculos** para identificar monedas en el flujo de video en tiempo real.
* Incorpora técnicas de preprocesamiento como **CLAHE** (Contrast Limited Adaptive Histogram Equalization) y **filtro Gaussiano** para mejorar la detección bajo diferentes condiciones de iluminación.
* Clasifica las monedas detectadas por su radio (requiere calibración previa).
* Calcula el valor total en **pesos colombianos (COP)** a partir de las monedas identificadas.

### 🙂 Detección de Rostros

* Emplea el clasificador **Haar Cascade** (`haarcascade_frontalface_alt2.xml`) para detectar rostros humanos.
* Cambia automáticamente a la **cámara frontal** al activar este modo.
* Dibuja **elipses** en tiempo real sobre los rostros detectados.
* Aplica **corrección de orientación** y **reflejo horizontal (espejo)** para una visualización adecuada con la cámara frontal.

---

## Tecnologías Utilizadas

* **Android SDK**
* **OpenCV para Android**
* **Java**

---

## Notas de Calibración y Uso

Para lograr una detección precisa de monedas:

* Es fundamental **calibrar** los parámetros de `HoughCircles` y los **radios esperados** de las monedas en el archivo `MainActivity.java`.
* Los valores iniciales en el código son solo ejemplos.
* Se recomienda hacer pruebas con las monedas a diferentes distancias (entre **15 cm y 40 cm**) en tu entorno real para ajustar correctamente los parámetros.

