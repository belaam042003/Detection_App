Detección de Monedas y Rostros con OpenCV en Android
Este proyecto es una aplicación Android que aprovecha la potencia de la librería OpenCV para realizar tareas de visión por computadora en tiempo real utilizando la cámara del dispositivo. La aplicación ofrece dos modos principales de funcionamiento: detección y suma del valor de monedas, y detección de rostros.

Funcionalidades Principales
Detección de Monedas:

Utiliza la Transformada de Hough para Círculos para identificar monedas en el flujo de video en tiempo real.

Incorpora preprocesamiento de imagen con CLAHE (Contrast Limited Adaptive Histogram Equalization) y filtro Gaussiano para mejorar la detección en diversas condiciones de iluminación.

Clasifica las monedas detectadas por su radio (calibración necesaria) y calcula el valor total en pesos colombianos (COP).

Detección de Rostros:

Emplea el clasificador Haar Cascade (haarcascade_frontalface_alt2.xml) para detectar rostros humanos.

Cambio automático a la cámara frontal: Al activar este modo, la aplicación cambia directamente a la cámara frontal para facilitar la detección de rostros.

Dibuja marcadores (elipses) sobre los rostros detectados en tiempo real.

Corrección de Orientación de Cámara: Ajusta automáticamente la orientación y el reflejo (efecto espejo) de la imagen de la cámara frontal para una visualización correcta.

Tecnologías Utilizadas
Android SDK

OpenCV para Android

Java

Notas de Calibración y Uso
Para una detección óptima de monedas, es crucial calibrar los parámetros de HoughCircles y los radios esperados de las monedas en el código fuente (MainActivity.java). Los valores iniciales proporcionados son ejemplos y deben ajustarse realizando pruebas en tu entorno específico y con las monedas a diferentes distancias (15 cm a 40 cm).
