# ==========================================
# GIAI ĐOẠN 1: Biên dịch và đóng gói ứng dụng (Build Stage)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Sao chép file cấu hình Maven pom.xml trước để tận dụng cache Docker layer cho dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Sao chép mã nguồn và thực hiện build package jar (bỏ qua chạy tests để tối ưu thời gian build)
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# GIAI ĐOẠN 2: Môi trường chạy thực tế (Runtime Stage)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Bảo mật: Tạo một nhóm và người dùng hệ thống không có quyền root (non-root user)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Sao chép file jar đã build từ Stage 1 sang Stage 2
COPY --from=build /app/target/*.jar app.jar

# Khai báo cổng chạy dịch vụ
EXPOSE 8080

# Cấu hình tham số JVM tối ưu cho Production, đặc biệt khi chạy trên tài nguyên hạn chế (Render/Koyeb Free Tier 512MB RAM):
# - MaxRAMPercentage=75.0: Cho phép JVM sử dụng tối đa 75% lượng RAM cấp cho container.
# - ActiveProcessorCount=1: Giới hạn nhận diện CPU core ảo là 1.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:ActiveProcessorCount=1", "-jar", "app.jar"]
