import 'package:flutter_test/flutter_test.dart';
import 'package:aplicacion_movil/features/recomendaciones/recomendaciones_model.dart';

void main() {
  group('CU26 - Recomendaciones IA Tests', () {
    test('Debe parsear correctamente el JSON de un evento recomendado', () {
      final json = {
        'idEvento': 101,
        'titulo': 'Taller de Machine Learning y Agentes',
        'descripcion': 'Aprende a construir agentes inteligentes con Gemini y Spring Boot',
        'fechaInicio': '2026-12-15T14:00:00Z',
        'fechaFin': '2026-12-15T18:00:00Z',
        'ubicacion': 'Campus Universitario - Pabellón A',
        'nombreCategoria': 'Tecnología',
        'aforoMaximo': 80,
        'inscritos': 35,
        'afinidad': 0.94,
        'motivoRecomendacion': 'Recomendado por tu interés recurrente en temas de Inteligencia Artificial.',
        'etiquetas': ['IA', 'Machine Learning', 'Agentes'],
      };

      final model = EventoRecomendadoModel.fromJson(json);

      expect(model.idEvento, 101);
      expect(model.titulo, 'Taller de Machine Learning y Agentes');
      expect(model.nombreCategoria, 'Tecnología');
      expect(model.afinidad, 0.94);
      expect(model.porcentajeAfinidad, 94);
      expect(model.motivoRecomendacion, contains('interés recurrente'));
      expect(model.etiquetas.length, 3);
      expect(model.etiquetas, contains('IA'));
      expect(model.inscritos, 35);
      expect(model.aforoMaximo, 80);
    });

    test('Debe calcular porcentajeAfinidad acotado entre 0 y 100', () {
      final modelAlto = EventoRecomendadoModel(
        idEvento: 1,
        titulo: 'Evento A',
        descripcion: 'Desc',
        fechaInicio: DateTime.now(),
        ubicacion: 'Lugar',
        nombreCategoria: 'Cat',
        inscritos: 10,
        afinidad: 1.25, // Mayor a 1.0
        motivoRecomendacion: 'Motivo',
        etiquetas: const [],
      );

      final modelBajo = EventoRecomendadoModel(
        idEvento: 2,
        titulo: 'Evento B',
        descripcion: 'Desc',
        fechaInicio: DateTime.now(),
        ubicacion: 'Lugar',
        nombreCategoria: 'Cat',
        inscritos: 5,
        afinidad: -0.1, // Menor a 0.0
        motivoRecomendacion: 'Motivo',
        etiquetas: const [],
      );

      expect(modelAlto.porcentajeAfinidad, 100);
      expect(modelBajo.porcentajeAfinidad, 0);
    });
  });
}
