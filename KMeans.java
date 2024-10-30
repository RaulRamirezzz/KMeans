import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class KMeans {

    // Lista para almacenar los datos de registros leídos desde el archivo CSV
    List<Record> data = new ArrayList<>();
    // Lista de clústeres creados durante el proceso
    List<Cluster> clusters = new ArrayList<>();
    // Mapa que asocia cada clúster con los registros que contiene
    Map<Cluster, List<Record>> clusterRecords = new HashMap<>();
    
    public static void main(String[] args) {
        
        int clusterNumber = 2; // Número de clústeres
        int maxIterations = 10; // Máximo de iteraciones
        int runs = 5; // Número de ejecuciones del algoritmo para obtener resultados estables
        KMeans demo = new KMeans();
        
        // Nombre del archivo CSV con los datos de entrada
        String filePath = "Mall_customers.csv";
        
        // Intenta leer el archivo CSV
        try {
            demo.readRecordsFromCSV(filePath);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo CSV: " + e.getMessage());
            return;
        }
        
        // Ejecuta el algoritmo K-Means múltiples veces para obtener resultados promediados
        for (int i = 0; i < runs; i++) {
            System.out.println("Ejecución " + (i + 1) + ":");
            demo.initializeClustersKMeansPlusPlus(clusterNumber); // Inicialización K-Means++
            demo.iterateClusters(maxIterations); // Ejecuta las iteraciones del algoritmo
            System.out.println();
        }
    }
    
    // Método para leer datos desde un archivo CSV
    private void readRecordsFromCSV(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Omitir la primera línea que contiene encabezados
            while ((line = br.readLine()) != null) {
                // Separa los valores por coma
                String[] values = line.split(",");
                // Extrae los valores de cada campo y crea un nuevo registro
                int id = Integer.parseInt(values[0].trim());
                int age = Integer.parseInt(values[2].trim());
                int income = Integer.parseInt(values[3].trim());
                int score = Integer.parseInt(values[4].trim());
                
                Record record = new Record(id, age, income, score);
                data.add(record); // Agrega el registro a la lista de datos
            }
        }
    }

    // Inicializa los clústeres utilizando el método K-means
    private void initializeClustersKMeansPlusPlus(int clusterNumber) {
        clusters.clear(); // Limpia cualquier clúster anterior
        clusterRecords.clear();
        
        Random random = new Random();
        // Selecciona el primer centroide aleatoriamente
        Record firstCentroid = data.get(random.nextInt(data.size()));
        Cluster initialCluster = new Cluster(1, firstCentroid.getAge(), firstCentroid.getIncome(), firstCentroid.getScore());
        clusters.add(initialCluster); // Agrega el primer clúster a la lista de clústeres
        clusterRecords.put(initialCluster, new ArrayList<>()); // Inicializa su lista de registros

        // Selecciona los demás centroides según el método k-means
        for (int i = 1; i < clusterNumber; i++) {
            double[] distances = new double[data.size()];
            double sumDistances = 0;

            // Calcula la distancia mínima de cada registro al clúster más cercano
            for (int j = 0; j < data.size(); j++) {
                Record record = data.get(j);
                double minDistance = Double.MAX_VALUE;

                // Encuentra la distancia más corta al clúster más cercano
                for (Cluster cluster : clusters) {
                    double distance = cluster.calculateDistance(record);
                    if (distance < minDistance) {
                        minDistance = distance;
                    }
                }
                distances[j] = minDistance;
                sumDistances += minDistance;
            }

            // Selecciona un nuevo centroide basado en una probabilidad proporcional a la distancia
            double randomPoint = random.nextDouble() * sumDistances;
            double cumulativeDistance = 0;
            Record newCentroid = null;

            for (int j = 0; j < data.size(); j++) {
                cumulativeDistance += distances[j];
                if (cumulativeDistance >= randomPoint) {
                    newCentroid = data.get(j);
                    break;
                }
            }

            // Agrega el nuevo centroide como un clúster
            if (newCentroid != null) {
                Cluster cluster = new Cluster(i + 1, newCentroid.getAge(), newCentroid.getIncome(), newCentroid.getScore());
                clusters.add(cluster);
                clusterRecords.put(cluster, new ArrayList<>());
            }
        }
    }

    // Ejecuta las iteraciones del algoritmo K-Means hasta que se alcance la convergencia
    private void iterateClusters(int maxIterations) {
        boolean converged = false;
        int iteration = 1;

        // Repite hasta que se alcance la convergencia o el máximo de iteraciones
        while (!converged && iteration <= maxIterations) {
            clearClusterAssignments(); // Limpia las asignaciones previas de cada clúster
            assignRecordsToClusters(); // Asigna cada registro al clúster más cercano
            converged = updateCentroids(); // Recalcula los centroides y verifica convergencia
            printClusterPercentages(iteration); // Imprime el porcentaje de registros en cada clúster
            iteration++;
        }
    }

    // Limpia las asignaciones de registros a los clústeres en cada iteración
    private void clearClusterAssignments() {
        for (Cluster cluster : clusters) {
            clusterRecords.get(cluster).clear();
        }
    }

    // Asigna cada registro al clúster más cercano en base a la distancia
    private void assignRecordsToClusters() {
        for (Record record : data) {
            Cluster closestCluster = null;
            double minDistance = Double.MAX_VALUE;

            // Encuentra el clúster con la menor distancia al registro actual
            for (Cluster cluster : clusters) {
                double distance = cluster.calculateDistance(record);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestCluster = cluster;
                }
            }
            
            // Agrega el registro al clúster más cercano
            if (closestCluster != null) {
                clusterRecords.get(closestCluster).add(record);
                record.setClusterNumber(closestCluster.getClusterNumber()); // Actualiza el número de clúster del registro
            }
        }
    }

    // Recalcula los centroides de cada clúster y verifica si hubo cambio
    private boolean updateCentroids() {
        boolean isConverged = true;

        // Recalcula el centroide de cada clúster
        for (Cluster cluster : clusters) {
            int previousAge = cluster.getAgeCentroid();
            int previousIncome = cluster.getIncomeCentroid();
            int previousScore = cluster.getScoreCentroid();
            
            List<Record> records = clusterRecords.get(cluster);
            int newAgeCentroid = 0, newIncomeCentroid = 0, newScoreCentroid = 0;

            // Suma los valores de los registros dentro del clúster
            for (Record record : records) {
                newAgeCentroid += record.getAge();
                newIncomeCentroid += record.getIncome();
                newScoreCentroid += record.getScore();
            }

            // Calcula el promedio para obtener el nuevo centroide
            int size = records.size();
            if (size > 0) {
                newAgeCentroid /= size;
                newIncomeCentroid /= size;
                newScoreCentroid /= size;
            }

            // Actualiza los centroides del clúster
            cluster.setAgeCentroid(newAgeCentroid);
            cluster.setIncomeCentroid(newIncomeCentroid);
            cluster.setScoreCentroid(newScoreCentroid);

            // Comprueba si los centroides han cambiado
            if (previousAge != newAgeCentroid || previousIncome != newIncomeCentroid || previousScore != newScoreCentroid) {
                isConverged = false;
            }
        }
        
        return isConverged; // Retorna verdadero si todos los centroides permanecen iguales
    }

    // Muestra el porcentaje de datos en cada clúster para cada iteración
    private void printClusterPercentages(int iteration) {
        int totalRecords = data.size();
        System.out.printf("Iteración %d:\n", iteration);

        for (Map.Entry<Cluster, List<Record>> entry : clusterRecords.entrySet()) {
            int clusterSize = entry.getValue().size();
            double percentage = ((double) clusterSize / totalRecords) * 100;
            System.out.printf("Cluster %d: %.2f%%\n", 
                              entry.getKey().getClusterNumber(), percentage);
        }
        System.out.println();
    }
}
