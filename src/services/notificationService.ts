import api, { unwrap } from "@/lib/api";

export interface Notification {
  id: string;
  userId: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export const notificationService = {
  async getMyNotifications(): Promise<Notification[]> {
    const res = await api.get("/notifications", { params: { size: 50 } });
    const data = unwrap<{ content: Notification[] }>(res);
    return data.content ?? [];
  },

  async getUnreadCount(): Promise<number> {
    const res = await api.get("/notifications/unread-count");
    return unwrap<number>(res);
  },

  async markAsRead(id: string): Promise<void> {
    await api.patch(`/notifications/${id}/read`);
  },

  async markAllAsRead(): Promise<void> {
    await api.patch("/notifications/read-all");
  },
};
