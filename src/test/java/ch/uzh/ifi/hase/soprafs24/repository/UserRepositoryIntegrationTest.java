package ch.uzh.ifi.hase.soprafs24.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    assertNotNull(found.getCreated());
  }

  @Test
  @Disabled("test creation date")
  public void onUserInsert_checkCreatedTime_success() {
    // mock time
    Instant instant = Instant.parse("1984-02-26T00:00:00Z");
    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

    Mockito.when(clock.instant()).thenReturn(instant);
    Mockito.when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    // given
    User user = new User();
    user.setName("Firstname Lastname");
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);
    user.setToken("1");
    user.setPassword("psw");

    // insert user
    entityManager.persist(user);
    entityManager.flush();

    // when
    User found = userRepository.findByUsername(user.getUsername());

    assertEquals(found.getCreated(), user.getCreated());
    assertEquals(found.getCreated(), dateTime);
  }
}
