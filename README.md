# java-explore-with-me
Template repository for ExploreWithMe project.

https://github.com/lojiloo/java-explore-with-me/pull/3

Анна, привет! Реализовала комментарии. По моей задумке теперь можно:
1. Отмечать при создании или обновлении события, разрешены ли или нет комментарии для него;
2. Комментировать опубликованные события;
3. Ставить лайки на комментарии к событиям (если отправить повторный запрос на создание лайка -- лайк уберётся);
4. Отмечать комментарии как нежелательные;
5. Редактировать уже существующие комментарии (но появляется отметка о том, что комментарий был изменён);
6. При достижении определённого числа жалоб на комментарий, он отправляется на ревью к администратору;
7. Пользователи видят количество лайков, но не видят количество жалоб на комментарий;
8. Администратор видит и лайки, и жалобы для любого комментария (могут получить их по id, по событию или по пользователю);
9. При запросе комментариев (пользователя или к событию) можно выбрать способ сортировки: по лайкам, от старых к новым и от новых к старым
