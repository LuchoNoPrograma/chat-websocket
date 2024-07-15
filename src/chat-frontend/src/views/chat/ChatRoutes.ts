export default [
  {
    name: 'ChatApp',
    path: '/chat',
    component: () => import('@/views/chat/ChatView.vue'),
    children: [{
      path: '',
      name: 'ChatRoom',
      component: () => import('@/components/apps/chats/ChatRoom.vue')
    }, {
      path: '/room/:roomId',
      name: 'ChatDetail',
      component: () => import('@/components/apps/chats/ChatDetail.vue')
    }, {
      path: 'private',
      name: 'ChatPrivate',
      component: () => import('@/components/apps/chats/ChatDetailPrivate.vue')
    }]
  }
];