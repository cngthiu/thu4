# Library Borrow / Return Management (Spring Boot + jOOQ + MySQL)

# Library Borrow / Return Management (Spring Boot + jOOQ + MySQL)

Hệ thống quản lý sách, thành viên, mượn–trả, cron đánh dấu quá hạn và gửi email nhắc hạn.  
Tech: **Java 21, Spring Boot 3.x, jOOQ 3.19+, MySQL 8.x, Thymeleaf+Bootstrap, Spring Mail, Scheduler**.  
Không dùng JPA/Hibernate. Không barcode/QR. Không reservation/hold. Tz: `Asia/Ho_Chi_Minh`.

## 1 Kiến trúc

- **Web (Thymeleaf)**: `/books`, `/members`, `/loans` (list + CRUD, mượn nhanh, lọc).
- **REST API**:
  - Books: `GET /api/books`, `POST /api/books`, `PUT /api/books/{id}`, `DELETE /api/books/{id}`
  - Members: tương tự.
  - Loans: `POST /api/loans/borrow?bookId={id}&memberId={id}&days=14`, `POST /api/loans/{loanId}/return`, `GET /api/loans`
- **Service layer (jOOQ + DSLContext)**: BookService, MemberService, LoanService, NotificationService.
- **Schedulers**:
  - `OverdueScheduler` (`0 0 8 * * *`): đánh dấu quá hạn + tính phạt, tạo `NOTIFICATION` nếu chưa có.
  - `EmailNotificationScheduler` (`0 */5 * * * *`): claim/steal batch (50), gửi mail, archive kết quả.
- **DB**: xem `sql/001_ddl.sql`, `sql/002_sample_data.sql`.

## 2 Yêu cầu môi trường

- Java 21, Maven 3.9+
- MySQL 8.x (chạy `localhost:3306`)
- Tạo database và account có quyền trên `booksdb`.

## 3 Tạo database & DDL

```bash
mysql -u root -p < sql/001_ddl.sql
mysql -u root -p booksdb < sql/002_sample_data.sql
```

# Cây thư mục dự án

```
library-borrow-return/
├─ pom.xml
├─ README.md
├─ sql/
│  ├─ 001_ddl.sql
│  └─ 002_sample_data.sql
├─ src/
│  ├─ main/
│  │  ├─ java/com/example/library/
│  │  │  ├─ LibraryApplication.java
│  │  │  ├─ config/
│  │  │  │  ├─ JooqConfig.java
│  │  │  │  └─ MailConfig.java
│  │  │  ├─ controller/
│  │  │  │  ├─ api/
│  │  │  │  │  ├─ BookApiController.java
│  │  │  │  │  ├─ MemberApiController.java
│  │  │  │  │  └─ LoanApiController.java
│  │  │  │  └─ web/
│  │  │  │     ├─ BooksPageController.java
│  │  │  │     ├─ MembersPageController.java
│  │  │  │     └─ LoansPageController.java
│  │  │  ├─ dto/
│  │  │  │  ├─ BookDto.java
│  │  │  │  ├─ MemberDto.java
│  │  │  │  └─ LoanFilter.java
│  │  │  ├─ exception/
│  │  │  │  ├─ ApiError.java
│  │  │  │  └─ GlobalExceptionHandler.java
│  │  │  ├─ scheduler/
│  │  │  │  ├─ OverdueScheduler.java
│  │  │  │  └─ EmailNotificationScheduler.java
│  │  │  ├─ service/
│  │  │  │  ├─ BookService.java
│  │  │  │  ├─ MemberService.java
│  │  │  │  ├─ LoanService.java
│  │  │  │  └─ NotificationService.java
│  │  │  └─ util/
│  │  │     └─ Timezones.java
│  │  └─ resources/
│  │     ├─ application.properties
│  │     ├─ templates/
│  │     │  ├─ fragments/
│  │     │  │  └─ layout.html
│  │     │  ├─ books/
│  │     │  │  └─ index.html
│  │     │  ├─ members/
│  │     │  │  └─ index.html
│  │     │  └─ loans/
│  │     │     └─ index.html
│  │     └─ static/
│  │        └─ css/bootstrap.min.css
│  └─ test/java/ (trống, có thể thêm sau)
└─ jooq-codegen-config.xml

```

# 4 jOOQ codegen

- Cấu hình file jooq-codegen-config.xml và plugin trong pom.xml.
  Chạy: mvn -DskipTests -Pjooq-codegen generate-sources
- Sau khi generate, các class jOOQ sẽ có dưới target/generated-sources/jooq.

# 5 Build & Run

- Cấu hình src/main/resources/application.properties cho datasource và SMTP (nếu gửi mail thật).

```bash
mvn clean package
mvn spring-boot:run
```

## UI: http://localhost:8080/books , /members , /loans

# 6 Cấu hình SMTP (gửi email thật)

- Trong application.properties:
  spring.mail.host=smtp.example.com
  spring.mail.port=587
  spring.mail.username=you@example.com
  spring.mail.password=app-password
  spring.mail.properties.mail.smtp.auth=true
  spring.mail.properties.mail.smtp.starttls.enable=true
- Nếu chưa cấu hình SMTP, scheduler email chỉ log cảnh báo, không ném exception.

# 7 Cron & Cơ chế NOTIFICATION concurrency-safe

- claimBatch(processId, timeoutSec=300, limit=50):
  - Steal các bản ghi lock quá TIMEOUT_SEC & RETRY_COUNT < 3.
  - Claim bản ghi chưa lock & RETRY_COUNT < 3.
  - Tăng RETRY_COUNT, đặt PROCESS_ID, LOCKED_AT=NOW().
- Gửi mail xong:
  - Thành công: xóa khỏi NOTIFICATION, ghi NOTIFICATION_HISTORY(SUCCESS=true).
  - Thất bại: ghi LAST_ERROR, LAST_ATTEMPT_AT, bỏ lock; nếu RETRY_COUNT >= 3 → archiveExhausted vào NOTIFICATION_HISTORY(SUCCESS=false) rồi xóa khỏi NOTIFICATION.

---

# `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>library-borrow-return</artifactId>
  <version>1.0.0</version>
  <name>library-borrow-return</name>
  <description>Library Borrow/Return Management (Spring Boot + jOOQ + MySQL)</description>
  <packaging>jar</packaging>
  <properties>
    <java.version>21</java.version>
    <spring.boot.version>3.3.4</spring.boot.version>
    <jooq.version>3.19.9</jooq.version>
    <maven.compiler.release>21</maven.compiler.release>
    <mysql.driver.version>8.4.0</mysql.driver.version>

  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring.boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
      <groupId>org.jooq</groupId>
      <artifactId>jooq</artifactId>
      <version>${jooq.version}</version>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <configuration><release>${maven.compiler.release}</release></configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring.boot.version}</version>
      </plugin>
    <plugin>
      <groupId>org.jooq</groupId>
      <artifactId>jooq-codegen-maven</artifactId>
      <version>${jooq.version}</version>
      <executions>
        <execution>
          <id>generate-jooq</id>
          <phase>generate-sources</phase>
          <goals>
            <goal>generate</goal>
          </goals>
          <configuration>
            <!-- Trỏ tới file cấu hình đã có -->
            <configurationFile>jooq-codegen-config.xml</configurationFile>
          </configuration>
        </execution>
      </executions>
      <dependencies>
        <!-- Driver JDBC để plugin kết nối DB lúc generate -->
        <dependency>
          <groupId>com.mysql</groupId>
          <artifactId>mysql-connector-j</artifactId>
         <version>${mysql.driver.version}</version>

          <!-- version kế thừa từ dependency khai báo ở phần dependencies -->
        </dependency>
      </dependencies>
    </plugin>

    </plugins>
  </build>
  <profiles>
    <profile>
      <id>jooq-codegen</id>
      <activation><activeByDefault>false</activeByDefault></activation>
      <build>
        <plugins>
          <plugin>
          <groupId>org.jooq</groupId>
          <artifactId>jooq-codegen-maven</artifactId>
          <version>${jooq.version}</version>
          <executions>
            <execution>
              <id>generate-jooq</id>
              <phase>generate-sources</phase>
              <goals>
                <goal>generate</goal>
              </goals>
              <configuration>
                <!-- Trỏ tới file cấu hình đã có -->
                <configurationFile>jooq-codegen-config.xml</configurationFile>
              </configuration>
            </execution>
          </executions>
          <dependencies>
            <!-- Driver JDBC để plugin kết nối DB lúc generate -->
            <dependency>
              <groupId>com.mysql</groupId>
              <artifactId>mysql-connector-j</artifactId>
              <version>${mysql.driver.version}</version>
              <!-- version kế thừa từ dependency khai báo ở phần dependencies -->
            </dependency>
          </dependencies>
        </plugin>

        </plugins>
      </build>
    </profile>
  </profiles>
</project>
```

**SQL — sql/001_ddl.sql**

```
CREATE DATABASE IF NOT EXISTS booksdb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE booksdb;

CREATE TABLE IF NOT EXISTS AUTHOR (
  AUTHOR_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  NAME VARCHAR(255) NOT NULL,
  NATIONALITY VARCHAR(100) NULL,
  CONSTRAINT UK_AUTHOR_NAME UNIQUE (NAME)
);

CREATE TABLE IF NOT EXISTS CATEGORY (
  CATEGORY_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  NAME VARCHAR(100) NOT NULL,
  CONSTRAINT UK_CATEGORY_NAME UNIQUE (NAME)
);

CREATE TABLE IF NOT EXISTS BOOK (
  BOOK_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  TITLE VARCHAR(255) NOT NULL,
  AUTHOR_ID BIGINT NOT NULL,
  CATEGORY_ID BIGINT NOT NULL,
  PUBLISHER VARCHAR(255) NULL,
  PUBLISHED_YEAR INT NULL,
  ISBN VARCHAR(32) NULL,
  PRICE DECIMAL(12,2) NULL,
  STOCK INT NOT NULL DEFAULT 0,
  STATUS ENUM('AVAILABLE','UNAVAILABLE') NOT NULL DEFAULT 'AVAILABLE',
  INDEX IX_BOOK_TITLE (TITLE),
  INDEX IX_BOOK_AUTHOR (AUTHOR_ID),
  INDEX IX_BOOK_CATEGORY (CATEGORY_ID),
  CONSTRAINT UK_BOOK_ISBN UNIQUE (ISBN),
  CONSTRAINT FK_BOOK_AUTHOR FOREIGN KEY (AUTHOR_ID) REFERENCES AUTHOR(AUTHOR_ID)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT FK_BOOK_CATEGORY FOREIGN KEY (CATEGORY_ID) REFERENCES CATEGORY(CATEGORY_ID)
    ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS MEMBER (
  MEMBER_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  FULL_NAME VARCHAR(255) NOT NULL,
  EMAIL VARCHAR(255) NOT NULL,
  PHONE VARCHAR(30) NULL,
  STATUS ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT UK_MEMBER_EMAIL UNIQUE (EMAIL)
);

CREATE TABLE IF NOT EXISTS LOAN (
  LOAN_ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  BOOK_ID BIGINT NOT NULL,
  MEMBER_ID BIGINT NOT NULL,
  BORROW_DATE DATETIME NOT NULL,
  DUE_DATE DATETIME NOT NULL,
  RETURN_DATE DATETIME NULL,
  STATUS ENUM('BORROWED','OVERDUE','RETURNED') NOT NULL DEFAULT 'BORROWED',
  FINE_AMOUNT DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  INDEX IX_LOAN_MEMBER (MEMBER_ID, STATUS),
  INDEX IX_LOAN_BOOK (BOOK_ID, STATUS),
  INDEX IX_LOAN_DUE (DUE_DATE, STATUS),
  CONSTRAINT FK_LOAN_BOOK FOREIGN KEY (BOOK_ID) REFERENCES BOOK(BOOK_ID)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT FK_LOAN_MEMBER FOREIGN KEY (MEMBER_ID) REFERENCES MEMBER(MEMBER_ID)
    ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS NOTIFICATION (
  ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  MEMBER_ID BIGINT NOT NULL,
  EMAIL VARCHAR(255) NOT NULL,
  SUBJECT VARCHAR(255) NOT NULL,
  BODY TEXT NOT NULL,
  CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PROCESS_ID VARCHAR(36) NULL,
  RETRY_COUNT INT NOT NULL DEFAULT 0,
  LOCKED_AT DATETIME NULL,
  TIMEOUT_SEC INT NOT NULL DEFAULT 300,
  LAST_ERROR TEXT NULL,
  LAST_ATTEMPT_AT DATETIME NULL,
  INDEX IX_NOTIFICATION_PROCESS (PROCESS_ID),
  INDEX IX_NOTIFICATION_LOCK (LOCKED_AT),
  INDEX IX_NOTIFICATION_RETRY (RETRY_COUNT)
);

CREATE TABLE IF NOT EXISTS NOTIFICATION_HISTORY (
  ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  MEMBER_ID BIGINT NOT NULL,
  EMAIL VARCHAR(255) NOT NULL,
  SUBJECT VARCHAR(255) NOT NULL,
  BODY TEXT NOT NULL,
  SUCCESS BOOLEAN NOT NULL,
  ERROR_MESSAGE TEXT NULL,
  CREATED_AT DATETIME NOT NULL,
  ARCHIVED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX IX_NH_CREATED (CREATED_AT, SUCCESS)
);

```
