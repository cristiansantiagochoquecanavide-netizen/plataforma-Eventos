class ConvocatoriaModel {
  const ConvocatoriaModel({
    required this.id,
    required this.idEvento,
    required this.nombreEvento,
    required this.tipoPersonal,
    required this.descripcion,
    required this.cantidadRequerida,
    required this.fechaLimite,
    required this.estado,
    required this.fechaCreacion,
    required this.fechaActualizacion,
  });

  final int id;
  final int idEvento;
  final String nombreEvento;
  final String tipoPersonal;
  final String descripcion;
  final int cantidadRequerida;
  final DateTime fechaLimite;
  final String estado;
  final DateTime fechaCreacion;
  final DateTime fechaActualizacion;

  bool get vigente {
    final hoy = DateTime.now();
    return estado == 'ABIERTA' &&
        !fechaLimite.isBefore(DateTime(hoy.year, hoy.month, hoy.day));
  }

  factory ConvocatoriaModel.fromJson(Map<String, dynamic> json) =>
      ConvocatoriaModel(
        id: json['id'] as int,
        idEvento: json['idEvento'] as int,
        nombreEvento: json['nombreEvento'] as String,
        tipoPersonal: json['tipoPersonal'] as String,
        descripcion: json['descripcion'] as String,
        cantidadRequerida: json['cantidadRequerida'] as int,
        fechaLimite: DateTime.parse(json['fechaLimite'] as String),
        estado: json['estado'] as String,
        fechaCreacion: DateTime.parse(json['fechaCreacion'] as String),
        fechaActualizacion: DateTime.parse(
          json['fechaActualizacion'] as String,
        ),
      );
}

String fechaConvocatoria(DateTime fecha) =>
    '${fecha.day.toString().padLeft(2, '0')}/${fecha.month.toString().padLeft(2, '0')}/${fecha.year}';
