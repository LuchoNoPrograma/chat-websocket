const PublicRoutes = {
    path: '/public',
    component: () => import('@/layouts/blank/BlankLayout.vue'),
    children: [
        {
            name: 'ChatApp',
            path: '/chat',
            component: () => import('@/views/chat/ChatView.vue')
        }
    ]
};

export default PublicRoutes;