class PostulacionStaffModel {
  const PostulacionStaffModel({
    required this.id,
    required this.idUsuario,
    required this.idConvocatoria,
    required this.estado,
    required this.fechaPostulacion,
    required this.fechaActualizacion,
  });

  final int id;
  final int idUsuario;
  final int idConvocatoria;
  final String estado;
  final DateTime fechaPostulacion;
  final DateTime fechaActualizacion;

  factory PostulacionStaffModel.fromJson(Map<String, dynamic> json) =>
      PostulacionStaffModel(
        id: json['id'] as int,
        idUsuario: json['idUsuario'] as int,
        idConvocatoria: json['idConvocatoria'] as int,
        estado: json['estado'] as String,
        fechaPostulacion: DateTime.parse(json['fechaPostulacion'] as String),
        fechaActualizacion: DateTime.parse(
          json['fechaActualizacion'] as String,
        ),
      );
}
