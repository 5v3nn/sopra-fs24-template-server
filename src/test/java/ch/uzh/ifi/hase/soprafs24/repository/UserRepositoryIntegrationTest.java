package ch.uzh.ifi.hase.soprafs24.repository;

import static org.junit.jupiter.api.Assertions.*;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;

@DataJpaTest
public class UserRepositoryIntegrationTest {
  @Autowired private TestEntityManager entityManager;

  @Autowired private UserRepository userRepository;

  @MockBean private Clock clock;

  @Test
  public void findByUsername_success() {
    // given
    User user = new User();
    user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);
    user.setToken("1");
    user.setPassword("psw");
    user.setBirthday("1234");

    entityManager.persist(user);
    entityManager.flush();

    // when
    User found = userRepository.findByUsername(user.getUsername());

    // then
    assertNotNull(found.getId());
    assertEquals(found.getName(), user.getName());
    assertEquals(found.getUsername(), user.getUsername());
    assertEquals(found.getToken(), user.getToken());
    assertEquals(found.getStatus(), user.getStatus());
    assertEquals(found.getPassword(), user.getPassword());
    assertEquals(found.getBirthday(), user.getBirthday());
  }

  /**
   * validate that the created time is correctly set by comparing the time before and after the
   * insertion of the user
   */
  @Test
  public void onUserInsert_checkCreatedTime_success() {
    // given
    User user = new User();
    user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);
    user.setToken("1");
    user.setPassword("psw");
    user.setBirthday("1234");

    // time before
    LocalDateTime before = LocalDateTime.now();

    // insert user
    entityManager.persist(user);
    entityManager.flush();

    // when
    User found = userRepository.findByUsername(user.getUsername());

    // time after
    LocalDateTime after = LocalDateTime.now();

    // compare found created times with the time before and after the insert
    assertNotNull(found.getCreated());
    assertTrue(found.getCreated().isAfter(before) || found.getCreated().isEqual(before));
    assertTrue(found.getCreated().isBefore(after) || found.getCreated().isEqual(after));
  }
}
