package su.foxogram.repositories;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import su.foxogram.models.User;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
	@NotNull
	Optional<User> findById(long id);

	@NotNull
	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	@Override
	void delete(@NotNull User user);

	User findByAvatar(String avatar);
}
