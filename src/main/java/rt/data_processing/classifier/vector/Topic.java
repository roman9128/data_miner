package rt.data_processing.classifier.vector;

public record Topic(
        String label,
        float[] centroid,
        float[][] referenceEmbeddings) {
}