package su.foxogram.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "codes", indexes = {
		@Index(name = "idx_code_user_id", columnList = "userId", unique = true),
		@Index(name = "idx_code_value", columnList = "value", unique = true)
})
public class OTP {
	@Id()
	public long userId;

	@Column()
	public String type;

	@Column()
	public String value;

	@Column()
	public long issuedAt;

	@Column()
	public long expiresAt;

	public OTP() {
	}

	public OTP(long userId, String type, String value, long issuedAt, long expiresAt) {
		this.userId = userId;
		this.type = type;
		this.value = value;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}
}
