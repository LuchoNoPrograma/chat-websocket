const PublicRoutes = {
  path: '/public',
  component: () => import('@/layouts/blank/BlankLayout.vue'),
  children: [
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
      }]
    }
  ]
};

export default PublicRoutes;